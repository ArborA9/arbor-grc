import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Box, Button, InputAdornment, LinearProgress, Stack, TextField, Typography, type Theme } from '@mui/material'
import type { SystemStyleObject } from '@mui/system'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import {
  vendorPortalApi,
  type TprmAssessment,
  type TprmAssessmentItem,
  type ValidateVendorAccessResponse,
} from './api'
import {
  AttentionBanner,
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  FilterPill,
  NARRATIVE_FIELD_MAX_WIDTH,
  compactControlSx,
  controlActionSx,
} from './components/ui'
import VendorQuestionCard from './components/tprm/VendorQuestionCard'
import {
  VendorPortalAuthCard,
  VendorPortalAuthLayout,
  VendorPortalLoading,
  VendorPortalShell,
  VendorPortalSubmitted,
  VendorPortalTopActions,
  toolbarBtnSx,
} from './components/tprm/VendorPortalChrome'
import {
  answersSnapshot,
  controlCodeFromMessage,
  domainAnchor,
  evidenceState,
  formatPortalError,
  groupByDomain,
  itemContentKey,
  itemIncomplete,
  mergeAssessmentPreservingLocal,
  patchItem,
  returnedContentUnchanged,
  submitBlockers,
} from './components/tprm/vendorPortalItems'
import { useBeforeUnload } from './uiQuality/useBeforeUnload'
import { formatTenantDate, formatTenantDateTime } from './uiQuality/tenantLocale'
import { brand, semantic } from './theme'

type Stage = 'loading' | 'invalid' | 'email' | 'otp' | 'form' | 'submitted'
type ItemFilter = 'all' | 'incomplete' | 'evidence'

const LOCAL_PREVIEW_OTP = '1111'

function isLocalPreviewHost() {
  const h = window.location.hostname
  return h === 'localhost' || h === '127.0.0.1'
}

function useQueryParams() {
  return useMemo(() => {
    const params = new URLSearchParams(window.location.search)
    return { tenantId: params.get('tenant') ?? '', token: params.get('token') ?? '' }
  }, [])
}

function matchesQuery(it: TprmAssessmentItem, q: string) {
  if (!q) return true
  const hay = `${it.controlCode} ${it.requirement} ${it.description} ${it.domain}`.toLowerCase()
  return hay.includes(q)
}

export default function VendorPortal() {
  const { tenantId, token } = useQueryParams()
  const [stage, setStage] = useState<Stage>('loading')
  const [validation, setValidation] = useState<ValidateVendorAccessResponse | null>(null)
  const [otp, setOtp] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [previewOtp, setPreviewOtp] = useState<string | null>(null)
  const [otpDigits, setOtpDigits] = useState(6)
  const [emailTouched, setEmailTouched] = useState(false)
  const [vendorToken, setVendorToken] = useState<string | null>(null)
  const [assessment, setAssessment] = useState<TprmAssessment | null>(null)
  const [baseline, setBaseline] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [otpNotice, setOtpNotice] = useState('')
  const [otpTouched, setOtpTouched] = useState(false)
  const [focusCode, setFocusCode] = useState<string | null>(null)
  const [showSubmitErrors, setShowSubmitErrors] = useState(false)
  const [saveNotice, setSaveNotice] = useState('')
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [itemFilter, setItemFilter] = useState<ItemFilter>('all')
  const [search, setSearch] = useState('')
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({})
  const [dismissedReturnHint, setDismissedReturnHint] = useState(false)
  const [dismissedContributorHint, setDismissedContributorHint] = useState(false)
  /** Content keys at return hand-off (only for still-unchanged returned controls). */
  const returnBaselinesRef = useRef<Record<number, string>>({})

  const visibleItems = useMemo(
    () => assessment?.items.filter((it) => assessment.status !== 'RETURNED' || it.returned) ?? [],
    [assessment],
  )

  useEffect(() => {
    if (!assessment || assessment.status !== 'RETURNED') {
      returnBaselinesRef.current = {}
      return
    }
    const next = { ...returnBaselinesRef.current }
    for (const it of assessment.items) {
      if (it.returned && it.unchangedSinceReturn && next[it.id] === undefined) {
        next[it.id] = itemContentKey(it)
      }
    }
    returnBaselinesRef.current = next
  }, [assessment])

  const returnBaselines = returnBaselinesRef.current
  const portalBlockers = useMemo(
    () => submitBlockers(visibleItems, returnBaselines),
    [visibleItems, assessment, returnBaselines],
  )
  const incompleteCount = useMemo(
    () => visibleItems.filter((it) => itemIncomplete(it, returnBaselines[it.id])).length,
    [visibleItems, assessment, returnBaselines],
  )
  const formDirty =
    stage === 'form' &&
    assessment != null &&
    baseline !== '' &&
    answersSnapshot(assessment.items) !== baseline

  useBeforeUnload(formDirty)

  useEffect(() => {
    if (stage !== 'form') return
    const html = document.documentElement
    const prevHtml = html.style.overflow
    const prevBody = document.body.style.overflow
    html.style.overflow = 'hidden'
    document.body.style.overflow = 'hidden'
    return () => {
      html.style.overflow = prevHtml
      document.body.style.overflow = prevBody
    }
  }, [stage])

  const otpLen = previewOtp?.length || otpDigits

  useEffect(() => {
    if (!tenantId || !token) {
      setStage('invalid')
      return
    }
    vendorPortalApi
      .validate(tenantId, token)
      .then((res) => {
        setValidation(res)
        setStage(res.valid ? 'email' : 'invalid')
      })
      .catch((e) => {
        setError(formatPortalError(e))
        setStage('invalid')
      })
  }, [tenantId, token])

  useEffect(() => {
    if (!focusCode) return
    document.getElementById(`vq-${focusCode}`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [focusCode])

  const adoptAssessment = useCallback((a: TprmAssessment) => {
    setAssessment(a)
    setBaseline(answersSnapshot(a.items))
    setShowSubmitErrors(false)
    setFocusCode(null)
  }, [])

  useEffect(() => {
    setDismissedReturnHint(false)
  }, [assessment?.id, assessment?.status])

  useEffect(() => {
    setDismissedContributorHint(false)
  }, [assessment?.id, assessment?.canSubmit])

  const assessmentRef = useRef(assessment)
  assessmentRef.current = assessment

  const patchQuestion = useCallback((itemId: number, patch: Partial<TprmAssessmentItem>) => {
    setFocusCode(null)
    setAssessment((prev) => (prev ? patchItem(prev, itemId, patch) : prev))
  }, [])

  const onEvidenceUploaded = useCallback((server: TprmAssessment) => {
    setFocusCode(null)
    setAssessment((prev) => {
      const next = prev ? mergeAssessmentPreservingLocal(prev, server) : server
      const openItems = next.items.filter((it) => next.status !== 'RETURNED' || it.returned)
      if (submitBlockers(openItems, returnBaselinesRef.current).length === 0) {
        setError('')
        setShowSubmitErrors(false)
      }
      return next
    })
  }, [])

  const emailLooksValid = /[^\s@]+@[^\s@]+\.[^\s@]+/.test(inviteEmail.trim())
  const emailErr = emailTouched && !inviteEmail.trim()
    ? 'Email is required'
    : emailTouched && inviteEmail.trim() && !emailLooksValid
      ? 'Enter a valid email'
      : ''

  const applyIssuedOtp = (res: { previewOtp?: string | null } | null, resent: boolean) => {
    const fromApi = res?.previewOtp?.trim() || null
    if (fromApi) {
      setPreviewOtp(fromApi)
      setOtpDigits(fromApi.length)
      setOtpNotice('')
      return
    }
    if (isLocalPreviewHost()) {
      setPreviewOtp(LOCAL_PREVIEW_OTP)
      setOtpDigits(LOCAL_PREVIEW_OTP.length)
      setOtpNotice('')
      return
    }
    setPreviewOtp(null)
    setOtpDigits(6)
    setOtpNotice(
      resent
        ? 'A new one-time passcode has been sent to your email.'
        : 'A one-time passcode has been sent to your email.',
    )
  }

  const sendCode = async () => {
    setEmailTouched(true)
    if (!emailLooksValid) {
      setError('Enter the email this invite was sent to.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const res = await vendorPortalApi.requestOtp(tenantId, token, inviteEmail.trim())
      setOtp('')
      setOtpTouched(false)
      applyIssuedOtp(res, false)
      setStage('otp')
    } catch (e) {
      setError(formatPortalError(e))
    } finally {
      setBusy(false)
    }
  }

  const resendOtp = async () => {
    if (!emailLooksValid) {
      setStage('email')
      setError('Enter the email this invite was sent to.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const res = await vendorPortalApi.requestOtp(tenantId, token, inviteEmail.trim())
      applyIssuedOtp(res, true)
    } catch (e) {
      setError(formatPortalError(e))
    } finally {
      setBusy(false)
    }
  }

  const redeem = async () => {
    setOtpTouched(true)
    if (otp.length !== otpLen) {
      setError(`Enter the ${otpLen}-digit one-time passcode.`)
      return
    }
    setBusy(true)
    setError('')
    try {
      const res = await vendorPortalApi.redeem(tenantId, token, otp.trim())
      setVendorToken(res.token)
      const a = await vendorPortalApi.getAssessment(res.token)
      adoptAssessment(a)
      setStage(a.status === 'SUBMITTED' || a.status === 'COMPLETE' ? 'submitted' : 'form')
    } catch (e) {
      setError(formatPortalError(e))
    } finally {
      setBusy(false)
    }
  }

  const save = async () => {
    const current = assessmentRef.current
    if (!vendorToken || !current) return
    setBusy(true)
    setError('')
    setSaveNotice('')
    try {
      const a = await vendorPortalApi.saveAnswers(
        vendorToken,
        current.items.map((it) => ({
          itemId: it.id,
          vendorAnswer: it.vendorAnswer,
          justification: it.justification,
          evidenceRef: it.evidenceRef,
        })),
      )
      const merged = mergeAssessmentPreservingLocal(current, a, { preferServerAnswers: true })
      adoptAssessment({ ...merged, canSubmit: a.canSubmit ?? current.canSubmit })
      setSaveNotice('Progress saved. These answers are visible to the Organization before you submit.')
    } catch (e) {
      const msg = formatPortalError(e)
      setError(msg)
      throw e instanceof Error ? e : new Error(msg)
    } finally {
      setBusy(false)
    }
  }

  const runSubmit = async () => {
    const current = assessmentRef.current
    if (!vendorToken || !current) return
    const blockers = submitBlockers(
      current.items.filter((it) => current.status !== 'RETURNED' || it.returned),
      returnBaselinesRef.current,
    )
    if (blockers.length > 0) {
      setShowSubmitErrors(true)
      setConfirmSubmit(false)
      setFocusCode(blockers[0].code)
      // Stay on All so fixing a control does not remove it from the list mid-edit.
      setItemFilter('all')
      setError(
        `Complete ${blockers.length} control${blockers.length === 1 ? '' : 's'} before submit: ${blockers
          .slice(0, 3)
          .map((b) => `${b.code} (${b.reason})`)
          .join('; ')}${blockers.length > 3 ? '…' : ''}`,
      )
      return
    }
    setBusy(true)
    setError('')
    try {
      await vendorPortalApi.saveAnswers(
        vendorToken,
        current.items.map((it) => ({
          itemId: it.id,
          vendorAnswer: it.vendorAnswer,
          justification: it.justification,
          evidenceRef: it.evidenceRef,
        })),
      )
      const a = await vendorPortalApi.submit(vendorToken)
      setAssessment(a)
      setBaseline(answersSnapshot(a.items))
      setFocusCode(null)
      setShowSubmitErrors(false)
      setConfirmSubmit(false)
      setStage('submitted')
    } catch (e) {
      const msg = formatPortalError(e)
      setError(msg)
      setFocusCode(controlCodeFromMessage(msg))
      setShowSubmitErrors(true)
      setConfirmSubmit(false)
    } finally {
      setBusy(false)
    }
  }

  const q = search.trim().toLowerCase()
  const filteredItems = useMemo(() => {
    return visibleItems.filter((it) => {
      if (!matchesQuery(it, q)) return false
      if (itemFilter === 'incomplete') return itemIncomplete(it, returnBaselines[it.id])
      if (itemFilter === 'evidence') return evidenceState(it).missing
      return true
    })
  }, [visibleItems, q, itemFilter, returnBaselines, assessment])

  const domainGroups = useMemo(() => groupByDomain(filteredItems), [filteredItems])
  const allDomainGroups = useMemo(() => groupByDomain(visibleItems), [visibleItems])

  const answeredCount = visibleItems.filter((it) => it.vendorAnswer).length
  const evidenceOutstanding = visibleItems.filter((it) => evidenceState(it).missing)

  // Submit / Evidence filters hide questions once they are fixed. Leave the empty trap.
  useEffect(() => {
    if (stage !== 'form' || visibleItems.length === 0) return
    if (itemFilter === 'incomplete' && incompleteCount === 0) {
      setItemFilter('all')
    } else if (itemFilter === 'evidence' && evidenceOutstanding.length === 0) {
      setItemFilter('all')
    }
  }, [stage, itemFilter, incompleteCount, evidenceOutstanding.length, visibleItems.length])

  useEffect(() => {
    if (stage !== 'form' || visibleItems.length === 0) return
    if (portalBlockers.length > 0) return
    setShowSubmitErrors(false)
    setError((prev) => (prev && /before submit/i.test(prev) ? '' : prev))
    setFocusCode(null)
  }, [stage, visibleItems, portalBlockers.length, incompleteCount, evidenceOutstanding.length, answeredCount])

  const otpReady = otp.length === otpLen
  const otpErr = otpTouched && otp.length > 0 && otp.length < otpLen
    ? `Enter all ${otpLen} digits`
    : otpTouched && !otp
      ? 'Passcode is required'
      : ''

  const dueLabel = assessment?.dueDate ? formatTenantDate(assessment.dueDate) : null
  const expiresLabel = assessment?.expiresAt ? formatTenantDateTime(assessment.expiresAt) : null
  const postureTargetValue = assessment?.snapshotTarget == null
    ? null
    : `${Math.round(assessment.snapshotTarget * 1000) / 10}%`
  const metaBits = [
    assessment?.vendorName,
    assessment?.displayId,
    assessment?.questionnaireName,
    dueLabel ? `Due ${dueLabel}` : null,
    expiresLabel && !dueLabel ? `Access until ${expiresLabel}` : null,
  ].filter(Boolean) as string[]

  const scrollToDomain = (domain: string) => {
    setCollapsed((prev) => ({ ...prev, [domain]: false }))
    document.getElementById(domainAnchor(domain))?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const toggleDomain = (domain: string) => {
    setCollapsed((prev) => ({ ...prev, [domain]: !prev[domain] }))
  }

  const toolbar =
    stage === 'form' && assessment ? (
      <VendorPortalTopActions
        status={assessment.status}
        unsaved={formDirty}
        answered={answeredCount}
        total={visibleItems.length}
        evidenceOutstanding={evidenceOutstanding.length}
        formDirty={formDirty}
        busy={busy}
        canSubmit={assessment.canSubmit !== false}
        postureTarget={postureTargetValue}
        onSave={() => { void save().catch(() => undefined) }}
        onSubmit={() => {
          if (portalBlockers.length > 0) {
            setShowSubmitErrors(true)
            // Stay on All so fixing a control does not remove it from the list mid-edit.
            setItemFilter('all')
            setFocusCode(portalBlockers[0].code)
            setError(`Complete ${portalBlockers.length} control${portalBlockers.length === 1 ? '' : 's'} before submit.`)
            return
          }
          setConfirmSubmit(true)
        }}
      />
    ) : undefined

  const contextBar =
    stage === 'form' && assessment && vendorToken ? (
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={1}
        alignItems={{ md: 'center' }}
        sx={{
          px: { xs: 2, md: 3 },
          py: 1,
          borderTop: `1px solid ${brand.hairline}`,
          bgcolor: brand.paper,
        }}
      >
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ flex: 1, minWidth: 0 }} alignItems="center">
          <FilterPill
            label="All"
            count={visibleItems.length}
            active={itemFilter === 'all'}
            onClick={() => setItemFilter('all')}
            startIcon={<FilterListRoundedIcon sx={{ fontSize: 14 }} />}
          />
          <FilterPill label="Incomplete" count={incompleteCount} active={itemFilter === 'incomplete'} onClick={() => setItemFilter('incomplete')} />
          <FilterPill label="Evidence needed" count={evidenceOutstanding.length} active={itemFilter === 'evidence'} onClick={() => setItemFilter('evidence')} />
        </Stack>
        <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap justifyContent="flex-end">
          <TextField
            size="small"
            placeholder="Search questions"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            inputProps={{ 'aria-label': 'Search questions' }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
                </InputAdornment>
              ),
            }}
            sx={{ width: { xs: '100%', sm: 240 }, '& .MuiOutlinedInput-root': compactControlSx as SystemStyleObject<Theme> }}
          />
          <Button
            size="small"
            variant="outlined"
            startIcon={<FileDownloadOutlinedIcon sx={{ fontSize: 16 }} />}
            sx={toolbarBtnSx}
            disabled={busy}
            onClick={() => void vendorPortalApi.downloadQuestionnaire(vendorToken)}
          >
            Download Excel
          </Button>
          <Button
            size="small"
            variant="outlined"
            component="label"
            disabled={busy}
            startIcon={<UploadFileOutlinedIcon sx={{ fontSize: 16 }} />}
            sx={toolbarBtnSx}
          >
            Upload Excel
            <input
              hidden
              type="file"
              accept=".xlsx"
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (file) {
                  setBusy(true)
                  void vendorPortalApi
                    .uploadQuestionnaire(vendorToken, file)
                    .then((a) => {
                      adoptAssessment(a)
                      setSaveNotice('Excel uploaded. Progress is visible to the Organization.')
                    })
                    .catch((err) => setError(formatPortalError(err)))
                    .finally(() => setBusy(false))
                }
                e.target.value = ''
              }}
            />
          </Button>
        </Stack>
      </Stack>
    ) : undefined

  return (
    <VendorPortalShell
      toolbar={toolbar}
      subbar={contextBar}
      lockViewport={stage === 'form'}
      brandTitle={
        stage === 'form' && assessment
          ? (assessment.title || assessment.questionnaireName || 'Vendor questionnaire')
          : undefined
      }
      brandSubtitle={stage === 'form' && assessment ? (metaBits.join(' · ') || 'Arbor GRC') : undefined}
    >
      {stage === 'loading' && <VendorPortalLoading />}
      {stage === 'invalid' && (
        <VendorPortalAuthLayout>
          <VendorPortalAuthCard
            eyebrow="Vendor questionnaire"
            title="This link is not valid"
            subtitle={error || validation?.message || 'Ask the Organization to send a new invite.'}
          >
            <Typography sx={{ fontSize: '0.8125rem', color: brand.muted }}>
              Links expire, and each invite is tied to a specific email.
            </Typography>
          </VendorPortalAuthCard>
        </VendorPortalAuthLayout>
      )}
      {stage === 'email' && (
        <VendorPortalAuthLayout>
          <VendorPortalAuthCard
            eyebrow="Third-Party Assessment Project"
            title="Verify Your Email Address"
            subtitle="To access and complete the assigned project, enter the email address that received the invitation. A one-time verification code will be sent to that email address."
            footer="The passcode will be sent only to the invited email address."
          >
            <Stack spacing={1.5}>
              {error && <Alert severity="error">{error}</Alert>}
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="Email"
                  required
                  type="email"
                  value={inviteEmail}
                  autoFocus
                  error={Boolean(emailErr)}
                  helperText={emailErr}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  onBlur={() => setEmailTouched(true)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && emailLooksValid && !busy) void sendCode()
                  }}
                  slotProps={{
                    htmlInput: {
                      autoComplete: 'email',
                      'aria-label': 'Invite email',
                    },
                  }}
                />
              </Box>
              <BusyButton
                variant="contained"
                size="small"
                busy={busy}
                disabled={!emailLooksValid}
                disabledReason={!emailLooksValid ? 'Enter the invite email' : undefined}
                onClick={() => void sendCode()}
                sx={toolbarBtnSx}
              >
                Send Code
              </BusyButton>
            </Stack>
          </VendorPortalAuthCard>
        </VendorPortalAuthLayout>
      )}
      {stage === 'otp' && (
        <VendorPortalAuthLayout>
          <VendorPortalAuthCard
            eyebrow="Third-Party Assessment Project"
            title="Enter your passcode"
            subtitle="Enter the one-time verification code sent to the invited email address."
            footer="The passcode will be sent only to the invited email address."
          >
            <Stack spacing={1.5}>
              {error && <Alert severity="error">{error}</Alert>}
              {previewOtp && (
                <Alert severity="info">
                  Email is not sending. Use one-time passcode <strong>{previewOtp}</strong>
                </Alert>
              )}
              {otpNotice && <Alert severity="info">{otpNotice}</Alert>}
              <Box sx={{ width: 168 }}>
                <CompactTextField
                  label={`${otpLen}-digit code`}
                  required
                  value={otp}
                  autoFocus
                  error={Boolean(otpErr)}
                  helperText={otpErr}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, otpLen))}
                  onBlur={() => setOtpTouched(true)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && otpReady && !busy) void redeem()
                  }}
                  slotProps={{
                    htmlInput: {
                      maxLength: otpLen,
                      inputMode: 'numeric',
                      autoComplete: 'one-time-code',
                      pattern: '[0-9]*',
                      'aria-label': `${otpLen}-digit one-time passcode`,
                    },
                  }}
                  sx={{
                    '& .MuiOutlinedInput-input': {
                      letterSpacing: '0.28em',
                      fontFamily: 'ui-monospace, Menlo, monospace',
                      fontWeight: 600,
                    },
                  }}
                />
              </Box>
              <Stack direction="row" spacing={1}>
                <BusyButton
                  variant="contained"
                  size="small"
                  busy={busy}
                  disabled={!otpReady}
                  disabledReason={!otpReady ? 'Enter the one-time passcode' : undefined}
                  onClick={() => void redeem()}
                  sx={toolbarBtnSx}
                >
                  Continue
                </BusyButton>
                {!previewOtp && (
                  <BusyButton size="small" busy={busy} onClick={() => void resendOtp()} sx={controlActionSx}>
                    Resend OTP
                  </BusyButton>
                )}
              </Stack>
            </Stack>
          </VendorPortalAuthCard>
        </VendorPortalAuthLayout>
      )}
      {stage === 'submitted' && (
        <VendorPortalSubmitted
          title={assessment?.title || assessment?.questionnaireName || assessment?.vendorName || ''}
        />
      )}
      {stage === 'form' && assessment && vendorToken && (
        <Box
          sx={{
            flex: 1,
            minHeight: 0,
            display: 'flex',
            overflow: 'hidden',
          }}
        >
          <Box
            component="nav"
            aria-label="Questionnaire domains"
            sx={{
              display: { xs: 'none', lg: 'flex' },
              flexDirection: 'column',
              flex: '0 0 248px',
              width: 248,
              minWidth: 248,
              minHeight: 0,
              overflowY: 'auto',
              overflowX: 'hidden',
              bgcolor: brand.sidebar,
              borderRight: `1px solid ${brand.goldMid}`,
              px: 1.25,
              py: 1.5,
            }}
          >
            <Typography sx={{ fontSize: '0.625rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: brand.goldDark, mb: 1, flexShrink: 0 }}>
              Sections
            </Typography>
            <Stack spacing={0.25}>
              {allDomainGroups.map((group) => {
                const done = group.items.filter((it) => it.vendorAnswer && !evidenceState(it).missing).length
                const pct = group.items.length === 0 ? 0 : Math.round((done / group.items.length) * 100)
                return (
                  <Box
                    key={group.domain || 'questions'}
                    component="button"
                    type="button"
                    onClick={() => scrollToDomain(group.domain)}
                    sx={{
                      display: 'block',
                      width: '100%',
                      textAlign: 'left',
                      border: 0,
                      bgcolor: 'transparent',
                      p: 1,
                      borderRadius: 1,
                      cursor: 'pointer',
                      font: 'inherit',
                      '&:hover': { bgcolor: brand.sidebarHover },
                    }}
                  >
                    <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.ink, lineHeight: 1.3 }}>
                      {group.domain || 'Questions'}
                    </Typography>
                    <Typography sx={{ fontSize: '0.625rem', color: brand.goldDark, mt: 0.25 }}>
                      {done} of {group.items.length}
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={pct}
                      sx={{
                        mt: 0.6,
                        height: 3,
                        borderRadius: 1,
                        bgcolor: 'rgba(184, 134, 11, 0.18)',
                        '& .MuiLinearProgress-bar': { bgcolor: pct === 100 ? semantic.risk.low : brand.gold },
                      }}
                    />
                  </Box>
                )
              })}
            </Stack>
          </Box>

          <Box
            sx={{
              flex: 1,
              minWidth: 0,
              minHeight: 0,
              overflowY: 'auto',
              overflowX: 'hidden',
              px: { xs: 2, md: 3 },
              py: 1.5,
              bgcolor: brand.canvas,
            }}
          >
            {assessment.canSubmit === false && !dismissedContributorHint && (
              <AttentionBanner
                tone="info"
                emphasis="Contributor"
                message="Only the Vendor primary SPOC can submit. You can still save progress."
                onDismiss={() => setDismissedContributorHint(true)}
              />
            )}
            {assessment.status === 'RETURNED' && !dismissedReturnHint && (
              <AttentionBanner
                tone="warning"
                emphasis="Returned for update"
                message="Update each returned control (answer, justification, or evidence) before submit. Submitting without a change is not allowed."
                onDismiss={() => setDismissedReturnHint(true)}
              />
            )}
            {saveNotice && (
              <AttentionBanner
                tone="success"
                emphasis="Saved"
                message={saveNotice}
                onDismiss={() => setSaveNotice('')}
              />
            )}
            {showSubmitErrors && portalBlockers.length > 0 && (
              <AttentionBanner
                tone="warning"
                emphasis="Incomplete"
                message={`${portalBlockers.length} control(s) still need an update, answer, justification, or evidence file before submit.`}
                onDismiss={() => setShowSubmitErrors(false)}
              />
            )}
            {error && (
              <AttentionBanner
                tone="error"
                emphasis={focusCode ?? undefined}
                message={error}
                actionLabel={focusCode ? `Go to ${focusCode}` : undefined}
                onAction={focusCode ? () => document.getElementById(`vq-${focusCode}`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }) : undefined}
                onDismiss={() => {
                  setError('')
                  setFocusCode(null)
                }}
              />
            )}

            {visibleItems.length === 0 ? (
              <Box sx={{ bgcolor: brand.paper, border: `1px solid ${brand.border}`, borderRadius: 1.5, p: 3 }}>
                <Typography variant="body2" color="text.secondary">
                  There are no open controls on this questionnaire.
                </Typography>
              </Box>
            ) : domainGroups.length === 0 ? (
              <Box sx={{ bgcolor: brand.paper, border: `1px solid ${brand.border}`, borderRadius: 1.5, p: 3 }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1.25 }}>
                  No questions match this filter.
                </Typography>
                <Button
                  size="small"
                  variant="outlined"
                  sx={controlActionSx}
                  onClick={() => {
                    setItemFilter('all')
                    setSearch('')
                  }}
                >
                  Show All Questions
                </Button>
              </Box>
            ) : (
              <Stack spacing={2.5}>
                {domainGroups.map((group) => {
                  const domain = group.domain || 'Questions'
                  const isCollapsed = Boolean(collapsed[group.domain])
                  const needEvidence = group.items.filter((it) => it.evidenceMandatory).length
                  return (
                    <Box key={group.domain || 'questions'} id={domainAnchor(group.domain)} sx={{ scrollMarginTop: 16 }}>
                      <Box
                        component="button"
                        type="button"
                        onClick={() => toggleDomain(group.domain)}
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 1,
                          width: '100%',
                          border: 0,
                          bgcolor: 'transparent',
                          p: 0,
                          mb: 1.25,
                          cursor: 'pointer',
                          font: 'inherit',
                          textAlign: 'left',
                        }}
                      >
                        <ExpandMoreRoundedIcon
                          sx={{
                            fontSize: 22,
                            color: brand.goldDark,
                            transform: isCollapsed ? 'rotate(-90deg)' : 'none',
                            transition: 'transform .15s ease',
                          }}
                        />
                        <Box sx={{ minWidth: 0, flex: 1 }}>
                          <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.ink, letterSpacing: '-0.01em' }}>
                            {domain}
                          </Typography>
                          <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight }}>
                            {group.items.length} question{group.items.length === 1 ? '' : 's'}
                            {needEvidence ? ` · ${needEvidence} require evidence` : ''}
                          </Typography>
                        </Box>
                      </Box>
                      {!isCollapsed && (
                        <Stack spacing={1.5}>
                          {group.items.map((it) => (
                            <VendorQuestionCard
                              key={it.id}
                              it={it}
                              vendorToken={vendorToken}
                              showErrors={showSubmitErrors}
                              updateRequired={returnedContentUnchanged(it, returnBaselines[it.id])}
                              onPatch={(id, patch) => {
                                setSaveNotice('')
                                patchQuestion(id, patch)
                              }}
                              onEvidenceUploaded={onEvidenceUploaded}
                              onError={setError}
                            />
                          ))}
                        </Stack>
                      )}
                    </Box>
                  )
                })}
              </Stack>
            )}
          </Box>
        </Box>
      )}
      <ConfirmActionDialog
        open={confirmSubmit}
        title="Submit questionnaire?"
        message="After submit you cannot change answers on this link. Save progress first if you still need to edit."
        confirmLabel={busy ? 'Submitting…' : 'Submit'}
        confirmColor="primary"
        busy={busy}
        onCancel={() => setConfirmSubmit(false)}
        onConfirm={() => void runSubmit()}
      />
    </VendorPortalShell>
  )
}
