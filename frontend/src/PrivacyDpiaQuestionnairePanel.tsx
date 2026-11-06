import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditRoundedIcon from '@mui/icons-material/EditRounded'
import {
  api,
  type DpiaQuestionnaire,
  type DpiaQuestionnaireQuestion,
  type DpiaResponseType,
  type Session,
  type UpsertDpiaQuestionBody,
} from './api'
import StatusChip from './components/StatusChip'
import {
  CompactTextField,
  ConfirmActionDialog,
  FormSelect,
  PageLoading,
  RegisterToolbar,
  SettingsCard,
  SimpleFormLayout,
} from './components/ui'
import { NARRATIVE_FIELD_MAX_WIDTH, twoColFieldSx } from './components/ui/formLayout'
import { readOnlyCaptionSx, TYPE_VALUE } from './components/ui/FormFieldLabel'

/** Wider than the default simple form so Name + Description can share a roomy row. */
const QUESTIONNAIRE_FORM_MAX_WIDTH = 960
import { useAppShellPage } from './components/AppShellPageContext'
import { useListReady } from './uiQuality/useListReady'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import { brand } from './theme'

const RESPONSE_TYPES: { value: DpiaResponseType; label: string }[] = [
  { value: 'FREE_TEXT', label: 'Free Text' },
  { value: 'FREE_TEXT_ATTACHMENT', label: 'Free Text with Attachment' },
  { value: 'YES_NO', label: 'Yes / No' },
  { value: 'YES_NO_PARTLY', label: 'Yes / No / Partly' },
  { value: 'MULTI_SELECT', label: 'Multi-select' },
  { value: 'CONFIRMATION', label: 'Confirmation' },
]

function emptyQuestionForm(): UpsertDpiaQuestionBody {
  return {
    code: '',
    sectionName: '',
    principle: '',
    questionText: '',
    guidance: '',
    responseType: 'FREE_TEXT',
    options: [],
    sortOrder: 0,
    required: false,
  }
}

export default function PrivacyDpiaQuestionnairePanel({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const isAdmin = session.roles.includes('OWNER') || session.roles.includes('ADMIN')
  const guardApi = useOptionalUnsavedChangesGuardApi()

  const [questionnaires, setQuestionnaires] = useState<DpiaQuestionnaire[]>([])
  const { listReady, markReady } = useListReady()
  const [search, setSearch] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<DpiaQuestionnaire | null>(null)
  const [creatingQuestionnaire, setCreatingQuestionnaire] = useState(false)
  const [editingQuestionnaireMeta, setEditingQuestionnaireMeta] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState<DpiaQuestionnaireQuestion | null>(null)
  const [addingQuestion, setAddingQuestion] = useState(false)
  const [busy, setBusy] = useState(false)

  // Questionnaire create/edit form
  const [qForm, setQForm] = useState({ name: '', description: '' })
  // Question form
  const [questionForm, setQuestionForm] = useState<UpsertDpiaQuestionBody>(emptyQuestionForm)
  const [optionsText, setOptionsText] = useState('')
  // Delete confirm
  const [deleteQuestionId, setDeleteQuestionId] = useState<number | null>(null)
  const [deleteQuestionnaire, setDeleteQuestionnaire] = useState<DpiaQuestionnaire | null>(null)

  const load = useCallback(async () => {
    try {
      const qs = await api.listDpiaQuestionnaires()
      setQuestionnaires(qs)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load questionnaires')
    } finally {
      markReady()
    }
  }, [onError, markReady])

  const loadDetail = useCallback(
    async (id: number) => {
      try {
        const d = await api.getDpiaQuestionnaire(id)
        setDetail(d)
        setQForm({ name: d.name, description: d.description ?? '' })
      } catch (e) {
        onError(e instanceof Error ? e.message : 'Failed to load questionnaire')
        setSelectedId(null)
      }
    },
    [onError],
  )

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    setEditingQuestionnaireMeta(false)
    if (selectedId != null) void loadDetail(selectedId)
    else setDetail(null)
  }, [selectedId, loadDetail])

  const filteredList = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return questionnaires
    return questionnaires.filter((qn) => qn.name.toLowerCase().includes(q))
  }, [questionnaires, search])

  const goToQuestionnaireList = useCallback(() => {
    setCreatingQuestionnaire(false)
    setSelectedId(null)
    setDetail(null)
    setEditingQuestionnaireMeta(false)
    setEditingQuestion(null)
    setAddingQuestion(false)
    setQForm({ name: '', description: '' })
    setQuestionForm(emptyQuestionForm())
    setOptionsText('')
  }, [])

  const goToQuestionnaireDetail = useCallback(() => {
    setEditingQuestionnaireMeta(false)
    setEditingQuestion(null)
    setAddingQuestion(false)
    setQuestionForm(emptyQuestionForm())
    setOptionsText('')
    if (detail) setQForm({ name: detail.name, description: detail.description ?? '' })
  }, [detail])

  const startEditQuestionnaireMeta = () => {
    if (!detail) return
    setQForm({ name: detail.name, description: detail.description ?? '' })
    setEditingQuestionnaireMeta(true)
  }

  const cancelEditQuestionnaireMeta = () => {
    if (detail) setQForm({ name: detail.name, description: detail.description ?? '' })
    setEditingQuestionnaireMeta(false)
  }

  // Shell gold back button appears when breadcrumbRoot is set (same as ROPA / BIA).
  const pageTitle = addingQuestion
    ? 'Add Question'
    : editingQuestion
      ? 'Edit Question'
      : creatingQuestionnaire
        ? 'New Questionnaire'
        : detail
          ? detail.name
          : 'DPIA Questionnaires'

  const breadcrumbRoot =
    addingQuestion || editingQuestion
      ? {
          label: detail?.name ?? 'Questionnaire',
          onClick: () => guardApi.requestNavigation(goToQuestionnaireDetail),
        }
      : detail || creatingQuestionnaire
        ? {
            label: 'DPIA Questionnaires',
            onClick: () => guardApi.requestNavigation(goToQuestionnaireList),
          }
        : undefined

  useAppShellPage(
    {
      title: pageTitle,
      breadcrumbTrail: creatingQuestionnaire
        ? 'New'
        : addingQuestion
          ? 'Add'
          : editingQuestion
            ? 'Edit'
            : undefined,
      breadcrumbRoot,
      onTitleClick: breadcrumbRoot?.onClick,
      actions:
        detail && !addingQuestion && !editingQuestion && !creatingQuestionnaire ? (
          <StatusChip label={detail.active ? 'Active' : 'Inactive'} tone={detail.active ? 'success' : 'neutral'} />
        ) : undefined,
    },
    [
      pageTitle,
      breadcrumbRoot,
      detail,
      creatingQuestionnaire,
      addingQuestion,
      editingQuestion,
      editingQuestionnaireMeta,
    ],
  )

  const act = async (fn: () => Promise<unknown>, successMsg: string) => {
    setBusy(true)
    try {
      await fn()
      onOk(successMsg)
      await load()
      if (selectedId) await loadDetail(selectedId)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  // ----------------- Create Questionnaire -----------------
  const createDisabledReason = !qForm.name.trim() ? 'Name is required' : null

  const handleCreateQuestionnaire = async () => {
    if (createDisabledReason) return
    await act(async () => {
      const created = await api.createDpiaQuestionnaire({ name: qForm.name.trim(), description: qForm.description || null })
      setCreatingQuestionnaire(false)
      setQForm({ name: '', description: '' })
      setSelectedId(created.id)
    }, 'Questionnaire created')
  }

  // ----------------- Update Questionnaire -----------------
  const metaDirty =
    detail != null &&
    editingQuestionnaireMeta &&
    !creatingQuestionnaire &&
    !addingQuestion &&
    !editingQuestion &&
    (qForm.name.trim() !== detail.name || (qForm.description || '') !== (detail.description ?? ''))

  const updateDisabledReason = !editingQuestionnaireMeta
    ? null
    : !qForm.name.trim()
      ? 'Name is required'
      : !metaDirty
        ? 'No changes to save'
        : null

  const handleUpdateQuestionnaire = async () => {
    if (!detail || updateDisabledReason) return
    await act(async () => {
      await api.updateDpiaQuestionnaire(detail.id, {
        name: qForm.name.trim(),
        description: qForm.description || null,
        active: detail.active,
      })
      setEditingQuestionnaireMeta(false)
    }, 'Questionnaire updated')
  }

  const handleToggleActive = async () => {
    if (!detail) return
    await act(
      () =>
        api.updateDpiaQuestionnaire(detail.id, {
          name: detail.name,
          description: detail.description,
          active: !detail.active,
        }),
      detail.active ? 'Questionnaire deactivated' : 'Questionnaire activated',
    )
  }

  // ----------------- Question Form -----------------
  const questionSaveDisabledReason = !questionForm.code.trim()
    ? 'Code is required'
    : !questionForm.questionText.trim()
      ? 'Question text is required'
      : questionForm.responseType === 'MULTI_SELECT' && optionsText.trim() === ''
        ? 'Options are required for multi-select'
        : null

  const openAddQuestion = () => {
    const nextOrder = detail?.questions?.length ? Math.max(...detail.questions.map((q) => q.sortOrder)) + 1 : 1
    setQuestionForm({ ...emptyQuestionForm(), sortOrder: nextOrder })
    setOptionsText('')
    setAddingQuestion(true)
    setEditingQuestion(null)
  }

  const openEditQuestion = (q: DpiaQuestionnaireQuestion) => {
    setQuestionForm({
      code: q.code,
      sectionName: q.sectionName ?? '',
      principle: q.principle ?? '',
      questionText: q.questionText,
      guidance: q.guidance ?? '',
      responseType: q.responseType,
      options: q.options ?? [],
      sortOrder: q.sortOrder,
      required: q.required,
    })
    setOptionsText((q.options ?? []).join(', '))
    setEditingQuestion(q)
    setAddingQuestion(false)
  }

  const handleSaveQuestion = async () => {
    if (!detail || questionSaveDisabledReason) return
    const body: UpsertDpiaQuestionBody = {
      ...questionForm,
      options:
        questionForm.responseType === 'MULTI_SELECT'
          ? optionsText
              .split(',')
              .map((s) => s.trim())
              .filter(Boolean)
          : [],
    }
    await act(async () => {
      if (editingQuestion) {
        await api.updateDpiaQuestionnaireQuestion(detail.id, editingQuestion.id, body)
      } else {
        await api.addDpiaQuestionnaireQuestion(detail.id, body)
      }
      setAddingQuestion(false)
      setEditingQuestion(null)
      setQuestionForm(emptyQuestionForm())
      setOptionsText('')
    }, editingQuestion ? 'Question updated' : 'Question added')
  }

  const handleDeleteQuestion = async () => {
    if (!detail || deleteQuestionId == null) return
    await act(async () => {
      await api.deleteDpiaQuestionnaireQuestion(detail.id, deleteQuestionId)
      setDeleteQuestionId(null)
    }, 'Question deleted')
  }

  const handleDeleteQuestionnaire = async () => {
    if (!deleteQuestionnaire) return
    setBusy(true)
    try {
      await api.deleteDpiaQuestionnaire(deleteQuestionnaire.id)
      setDeleteQuestionnaire(null)
      if (selectedId === deleteQuestionnaire.id) {
        setSelectedId(null)
        setDetail(null)
      }
      onOk('Questionnaire deleted')
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Delete failed')
    } finally {
      setBusy(false)
    }
  }

  const createFormDirty =
    creatingQuestionnaire && (qForm.name.trim() !== '' || qForm.description.trim() !== '')
  const questionFormDirty = addingQuestion
    ? questionForm.code.trim() !== '' ||
      questionForm.questionText.trim() !== '' ||
      (questionForm.sectionName || '') !== '' ||
      (questionForm.principle || '') !== '' ||
      (questionForm.guidance || '') !== '' ||
      optionsText.trim() !== ''
    : editingQuestion != null &&
      (questionForm.code !== editingQuestion.code ||
        questionForm.questionText !== editingQuestion.questionText ||
        (questionForm.sectionName || '') !== (editingQuestion.sectionName ?? '') ||
        (questionForm.principle || '') !== (editingQuestion.principle ?? '') ||
        (questionForm.guidance || '') !== (editingQuestion.guidance ?? '') ||
        optionsText !== (editingQuestion.options ?? []).join(', ') ||
        questionForm.responseType !== editingQuestion.responseType ||
        Boolean(questionForm.required) !== Boolean(editingQuestion.required) ||
        Number(questionForm.sortOrder) !== Number(editingQuestion.sortOrder))
  const questionnaireDirty = createFormDirty || metaDirty || questionFormDirty
  const saveQuestionnaireOnLeave = metaDirty
    ? async () => {
        await handleUpdateQuestionnaire()
      }
    : questionFormDirty && !questionSaveDisabledReason
      ? async () => {
          await handleSaveQuestion()
        }
      : undefined
  useRegisterUnsavedChanges(
    questionnaireDirty
      ? {
          dirty: true,
          title: 'Unsaved questionnaire changes',
          message: saveQuestionnaireOnLeave
            ? 'You have unsaved questionnaire changes. Save to keep your work, or discard to leave without saving.'
            : 'You have unsaved questionnaire changes. Keep editing to stay on this screen, or discard to leave without saving.',
          onSaveDraft: saveQuestionnaireOnLeave,
          saveDraftLabel: 'Save',
        }
      : null,
  )

  // ----------------- Loading -----------------
  if (!listReady) return <PageLoading />

  // ----------------- Create Questionnaire Screen -----------------
  if (creatingQuestionnaire) {
    return (
      <SimpleFormLayout
        maxWidth={QUESTIONNAIRE_FORM_MAX_WIDTH}
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button onClick={() => guardApi.requestNavigation(() => setCreatingQuestionnaire(false))}>Cancel</Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(createDisabledReason)}
                onClick={() => void handleCreateQuestionnaire()}
              >
                Create
              </Button>
            </Stack>
            {createDisabledReason && (
              <Typography variant="caption" color="text.secondary">
                {createDisabledReason}
              </Typography>
            )}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="New Questionnaire">
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '240px minmax(0, 1fr)' },
              gap: 1.5,
              alignItems: 'start',
              width: '100%',
            }}
          >
            <CompactTextField
              label="Name"
              required
              value={qForm.name}
              onChange={(e) => setQForm({ ...qForm, name: e.target.value })}
              fieldIdPrefix="q"
            />
            <CompactTextField
              label="Description"
              value={qForm.description}
              onChange={(e) => setQForm({ ...qForm, description: e.target.value })}
              multiline
              minRows={5}
              fieldIdPrefix="q"
            />
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  // ----------------- Add/Edit Question Screen -----------------
  if (detail && (addingQuestion || editingQuestion)) {
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button onClick={() => guardApi.requestNavigation(goToQuestionnaireDetail)}>Cancel</Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(questionSaveDisabledReason)}
                onClick={() => void handleSaveQuestion()}
              >
                {editingQuestion ? 'Save' : 'Add'}
              </Button>
            </Stack>
            {questionSaveDisabledReason && (
              <Typography variant="caption" color="text.secondary">
                {questionSaveDisabledReason}
              </Typography>
            )}
          </Stack>
        }
      >
        <SettingsCard autoHeight title={editingQuestion ? 'Edit Question' : 'Add Question'}>
          <Stack spacing={1.5}>
            <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap alignItems="flex-start">
              <CompactTextField
                label="Code"
                required
                value={questionForm.code}
                onChange={(e) => setQuestionForm({ ...questionForm, code: e.target.value })}
                fieldIdPrefix="qq"
                maxWidth={120}
              />
              <CompactTextField
                label="Sort Order"
                type="number"
                value={String(questionForm.sortOrder ?? 0)}
                onChange={(e) => setQuestionForm({ ...questionForm, sortOrder: Number(e.target.value) || 0 })}
                fieldIdPrefix="qq"
                maxWidth={100}
              />
              <FormSelect
                label="Response Type"
                value={questionForm.responseType}
                options={RESPONSE_TYPES}
                onChange={(v) => setQuestionForm({ ...questionForm, responseType: v })}
                minWidth={220}
              />
              <FormSelect
                label="Required"
                value={questionForm.required ? 'YES' : 'NO'}
                options={[
                  { value: 'YES', label: 'Yes' },
                  { value: 'NO', label: 'No' },
                ]}
                onChange={(v) => setQuestionForm({ ...questionForm, required: v === 'YES' })}
                minWidth={120}
              />
            </Stack>
            <Box sx={twoColFieldSx}>
              <CompactTextField
                label="Section Name"
                value={questionForm.sectionName}
                onChange={(e) => setQuestionForm({ ...questionForm, sectionName: e.target.value })}
                fieldIdPrefix="qq"
              />
              <CompactTextField
                label="Principle"
                value={questionForm.principle}
                onChange={(e) => setQuestionForm({ ...questionForm, principle: e.target.value })}
                fieldIdPrefix="qq"
              />
            </Box>
            <CompactTextField
              label="Question Text"
              required
              value={questionForm.questionText}
              onChange={(e) => setQuestionForm({ ...questionForm, questionText: e.target.value })}
              multiline
              minRows={4}
              fieldIdPrefix="qq"
              maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
            />
            <CompactTextField
              label="Guidance"
              value={questionForm.guidance}
              onChange={(e) => setQuestionForm({ ...questionForm, guidance: e.target.value })}
              multiline
              minRows={3}
              fieldIdPrefix="qq"
              maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
            />
            {questionForm.responseType === 'MULTI_SELECT' && (
              <CompactTextField
                label="Options (comma-separated)"
                required
                value={optionsText}
                onChange={(e) => setOptionsText(e.target.value)}
                helperText="Enter options separated by commas"
                multiline
                minRows={2}
                fieldIdPrefix="qq"
                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
              />
            )}
          </Stack>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  // ----------------- Questionnaire Detail Screen -----------------
  if (detail) {
    const questions = detail.questions ?? []
    const sortedQuestions = [...questions].sort((a, b) => a.sortOrder - b.sortOrder)
    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <SettingsCard autoHeight title="Questionnaire Details" compact>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '240px minmax(0, 1fr)' },
              gap: 1.5,
              alignItems: 'start',
              width: '100%',
              maxWidth: QUESTIONNAIRE_FORM_MAX_WIDTH,
            }}
          >
            {editingQuestionnaireMeta ? (
              <>
                <CompactTextField
                  label="Name"
                  required
                  value={qForm.name}
                  onChange={(e) => setQForm({ ...qForm, name: e.target.value })}
                  disabled={detail.systemDefault}
                  fieldIdPrefix="qd"
                />
                <CompactTextField
                  label="Description"
                  value={qForm.description}
                  onChange={(e) => setQForm({ ...qForm, description: e.target.value })}
                  multiline
                  minRows={5}
                  fieldIdPrefix="qd"
                />
              </>
            ) : (
              <>
                <Box>
                  <Typography component="span" sx={readOnlyCaptionSx}>
                    Name
                  </Typography>
                  <Typography sx={{ fontSize: TYPE_VALUE, color: brand.ink, wordBreak: 'break-word' }}>
                    {detail.name}
                  </Typography>
                </Box>
                <Box>
                  <Typography component="span" sx={readOnlyCaptionSx}>
                    Description
                  </Typography>
                  <Typography
                    sx={{
                      fontSize: TYPE_VALUE,
                      color: brand.ink,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {detail.description?.trim() ? detail.description : '-'}
                  </Typography>
                </Box>
              </>
            )}
          </Box>
          {isAdmin && (
            <Stack spacing={0.5} sx={{ mt: 1.5 }} alignItems="flex-start">
              {editingQuestionnaireMeta ? (
                <Stack direction="row" spacing={1} alignItems="center">
                  <Button
                    variant="contained"
                    size="small"
                    disabled={busy || Boolean(updateDisabledReason)}
                    onClick={() => void handleUpdateQuestionnaire()}
                  >
                    Save
                  </Button>
                  <Button
                    size="small"
                    disabled={busy}
                    onClick={() =>
                      guardApi.requestNavigation(() => cancelEditQuestionnaireMeta())
                    }
                  >
                    Cancel
                  </Button>
                </Stack>
              ) : (
                <Stack direction="row" spacing={1} alignItems="center">
                  <Button variant="contained" size="small" disabled={busy} onClick={startEditQuestionnaireMeta}>
                    Edit
                  </Button>
                  {!detail.systemDefault && (
                    <Button size="small" disabled={busy} onClick={() => void handleToggleActive()}>
                      {detail.active ? 'Deactivate' : 'Activate'}
                    </Button>
                  )}
                  {!detail.systemDefault && (
                    <Button
                      size="small"
                      color="error"
                      disabled={busy}
                      onClick={() => setDeleteQuestionnaire(detail)}
                    >
                      Delete
                    </Button>
                  )}
                </Stack>
              )}
              {editingQuestionnaireMeta && updateDisabledReason && (
                <Typography variant="caption" color="text.secondary">
                  {updateDisabledReason}
                </Typography>
              )}
            </Stack>
          )}
        </SettingsCard>

        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            search={search}
            searchPlaceholder="Search questions"
            onSearchChange={setSearch}
            trailing={
              isAdmin ? (
                <Button
                  variant="contained"
                  size="small"
                  onClick={() => guardApi.requestNavigation(() => openAddQuestion())}
                >
                  Add Question
                </Button>
              ) : undefined
            }
          />
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ width: 60 }}>Order</TableCell>
                  <TableCell sx={{ width: 80 }}>Code</TableCell>
                  <TableCell>Section</TableCell>
                  <TableCell>Question</TableCell>
                  <TableCell sx={{ width: 140 }}>Response Type</TableCell>
                  <TableCell sx={{ width: 80 }}>Required</TableCell>
                  {isAdmin && <TableCell sx={{ width: 100 }}>Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {sortedQuestions.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={isAdmin ? 7 : 6}>
                      <Typography color="text.secondary" sx={{ py: 2 }}>
                        No questions yet. Use Add Question to start building the questionnaire.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {sortedQuestions
                  .filter((q) => !search.trim() || q.questionText.toLowerCase().includes(search.trim().toLowerCase()))
                  .map((q) => (
                    <TableRow key={q.id} hover>
                      <TableCell>{q.sortOrder}</TableCell>
                      <TableCell>{q.code}</TableCell>
                      <TableCell>{q.sectionName || '-'}</TableCell>
                      <TableCell>
                        <Tooltip title={q.questionText} arrow>
                          <Typography
                            sx={{
                              maxWidth: 320,
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                              fontSize: '0.8125rem',
                            }}
                          >
                            {q.questionText}
                          </Typography>
                        </Tooltip>
                      </TableCell>
                      <TableCell>
                        <StatusChip
                          label={RESPONSE_TYPES.find((r) => r.value === q.responseType)?.label ?? q.responseType}
                          tone="info"
                        />
                      </TableCell>
                      <TableCell>{q.required ? 'Yes' : 'No'}</TableCell>
                      {isAdmin && (
                        <TableCell>
                          <Stack direction="row" spacing={0.5}>
                            <IconButton
                              size="small"
                              onClick={() => guardApi.requestNavigation(() => openEditQuestion(q))}
                            >
                              <EditRoundedIcon fontSize="small" />
                            </IconButton>
                            <IconButton size="small" color="error" onClick={() => setDeleteQuestionId(q.id)}>
                              <DeleteOutlineRoundedIcon fontSize="small" />
                            </IconButton>
                          </Stack>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>

        <ConfirmActionDialog
          open={deleteQuestionId != null}
          title="Delete Question"
          message="Are you sure you want to delete this question? This action cannot be undone."
          confirmLabel="Delete"
          confirmColor="error"
          busy={busy}
          onCancel={() => setDeleteQuestionId(null)}
          onConfirm={() => void handleDeleteQuestion()}
        />
        <ConfirmActionDialog
          open={deleteQuestionnaire != null}
          title="Delete Questionnaire"
          message={
            deleteQuestionnaire
              ? `Delete "${deleteQuestionnaire.name}"? This removes the template and its questions. This cannot be undone.`
              : ''
          }
          confirmLabel="Delete"
          confirmColor="error"
          busy={busy}
          onCancel={() => setDeleteQuestionnaire(null)}
          onConfirm={() => void handleDeleteQuestionnaire()}
        />
      </Stack>
    )
  }

  // ----------------- Questionnaire List Screen -----------------
  return (
    <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search questionnaires"
          onSearchChange={setSearch}
          trailing={
            isAdmin ? (
              <Button
                variant="contained"
                size="small"
                onClick={() => {
                  setQForm({ name: '', description: '' })
                  setCreatingQuestionnaire(true)
                }}
              >
                New Questionnaire
              </Button>
            ) : undefined
          }
        />
        <TableContainer sx={{ flex: 1, minHeight: 0 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell sx={{ width: 100 }}>Questions</TableCell>
                <TableCell sx={{ width: 100 }}>Status</TableCell>
                {isAdmin && <TableCell sx={{ width: 72 }} align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredList.length === 0 && (
                <TableRow>
                  <TableCell colSpan={isAdmin ? 5 : 4}>
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                      {questionnaires.length === 0
                        ? 'No questionnaires yet.'
                        : 'No questionnaires match these filters.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {filteredList.map((q) => (
                <TableRow key={q.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelectedId(q.id)}>
                  <TableCell>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography component="span" sx={{ fontSize: '0.8125rem', fontWeight: 600 }}>
                        {q.name}
                      </Typography>
                      {q.systemDefault && <StatusChip label="Default" tone="info" />}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Typography
                      sx={{
                        maxWidth: 360,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        fontSize: '0.8125rem',
                        color: brand.muted,
                      }}
                    >
                      {q.description || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>{q.questionCount ?? 0}</TableCell>
                  <TableCell>
                    <StatusChip label={q.active ? 'Active' : 'Inactive'} tone={q.active ? 'success' : 'neutral'} />
                  </TableCell>
                  {isAdmin && (
                    <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                      {!q.systemDefault ? (
                        <IconButton
                          size="small"
                          color="error"
                          aria-label={`Delete ${q.name}`}
                          onClick={() => setDeleteQuestionnaire(q)}
                        >
                          <DeleteOutlineRoundedIcon fontSize="small" />
                        </IconButton>
                      ) : (
                        <Typography component="span" sx={{ fontSize: '0.75rem', color: brand.muted }}>
                          -
                        </Typography>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      <ConfirmActionDialog
        open={deleteQuestionnaire != null}
        title="Delete Questionnaire"
        message={
          deleteQuestionnaire
            ? `Delete "${deleteQuestionnaire.name}"? This removes the template and its questions. This cannot be undone.`
            : ''
        }
        confirmLabel="Delete"
        confirmColor="error"
        busy={busy}
        onCancel={() => setDeleteQuestionnaire(null)}
        onConfirm={() => void handleDeleteQuestionnaire()}
      />
    </Stack>
  )
}
