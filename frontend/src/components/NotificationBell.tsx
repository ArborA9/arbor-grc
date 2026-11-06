import { useCallback, useEffect, useState, type MouseEvent } from 'react'
import {
  Badge,
  Box,
  Button,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Menu,
  Stack,
  Typography,
} from '@mui/material'
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded'
import { api, type NotificationItem } from '../api'
import { brand } from '../theme'
import { formatTenantDateTime } from '../uiQuality/tenantLocale'

type Props = {
  onNavigate?: (navId: string) => void
  onOpenNotification?: (n: NotificationItem) => void
  onViewAll?: () => void
}

export default function NotificationBell({ onNavigate, onOpenNotification, onViewAll }: Props) {
  const [anchor, setAnchor] = useState<null | HTMLElement>(null)
  const [items, setItems] = useState<NotificationItem[]>([])
  const [unread, setUnread] = useState(0)

  const refresh = useCallback(() => {
    api.listNotificationsLatest()
      .then((r) => {
        setItems(r.notifications ?? [])
        setUnread(r.unreadCount ?? 0)
      })
      .catch(() => {
        /* bell is best-effort */
      })
  }, [])

  useEffect(() => {
    refresh()
    const id = window.setInterval(refresh, 60_000)
    return () => window.clearInterval(id)
  }, [refresh])

  const open = (e: MouseEvent<HTMLElement>) => {
    setAnchor(e.currentTarget)
    refresh()
  }

  const markOne = async (id: number) => {
    try {
      await api.markNotificationRead(id)
      refresh()
    } catch {
      /* ignore */
    }
  }

  const markAll = async () => {
    try {
      await api.markAllNotificationsRead()
      refresh()
    } catch {
      /* ignore */
    }
  }

  return (
    <>
      <IconButton
        size="small"
        aria-label="Notifications"
        onClick={open}
        sx={{ overflow: 'visible', mr: 0.75, pr: 0.75 }}
      >
        <Badge
          badgeContent={unread}
          color="error"
          max={99}
          overlap="circular"
          sx={{
            '& .MuiBadge-badge': {
              fontSize: '0.625rem',
              fontWeight: 700,
              minWidth: 16,
              height: 16,
              px: 0.4,
              lineHeight: '16px',
              top: 2,
              right: 1,
            },
          }}
        >
          <NotificationsNoneRoundedIcon sx={{ fontSize: 22, color: brand.inkSoft }} />
        </Badge>
      </IconButton>
      <Menu
        anchorEl={anchor}
        open={!!anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { width: 360, maxWidth: '92vw' } } }}
      >
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ px: 1.75, py: 1 }}>
          <Typography variant="subtitle2">Notifications</Typography>
          <Button size="small" disabled={unread === 0} onClick={markAll}>
            Mark All Read
          </Button>
        </Stack>
        <Divider />
        {items.length === 0 ? (
          <Box sx={{ px: 2, py: 3 }}>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              No notifications yet.
            </Typography>
          </Box>
        ) : (
          <List dense disablePadding sx={{ maxHeight: 360, overflow: 'auto' }}>
            {items.map((n) => (
              <ListItemButton
                key={n.id}
                alignItems="flex-start"
                onClick={() => {
                  if (n.unread) void markOne(n.id)
                  if (onOpenNotification) {
                    onOpenNotification(n)
                    setAnchor(null)
                  } else if (n.link && onNavigate) {
                    onNavigate(n.link)
                    setAnchor(null)
                  }
                }}
                sx={{ bgcolor: n.unread ? brand.accentMuted : 'transparent', py: 1.1 }}
              >
                <ListItemText
                  primary={
                    <Typography variant="body2" fontWeight={n.unread ? 650 : 500}>
                      {n.title}
                    </Typography>
                  }
                  secondary={
                    <>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.25 }}>
                        {n.body}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.35 }}>
                        {formatTenantDateTime(n.createdAt)}
                      </Typography>
                    </>
                  }
                />
              </ListItemButton>
            ))}
          </List>
        )}
        <Divider />
        <Box sx={{ px: 1.5, py: 1 }}>
          <Button
            fullWidth
            size="small"
            onClick={() => {
              onViewAll?.()
              setAnchor(null)
            }}
          >
            View All
          </Button>
        </Box>
      </Menu>
    </>
  )
}
