import type { ReactNode } from 'react'
import PageLoading, { EmptyRegister } from './PageLoading'
import RegisterPagination from './RegisterPagination'

/** Loading vs empty vs table. Pagination only when count exceeds the page size. */
export default function RegisterListFrame({
  ready,
  loading,
  count,
  emptyMessage,
  emptyAction,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  children,
}: {
  ready: boolean
  loading?: boolean
  count: number
  emptyMessage: string
  emptyAction?: ReactNode
  page: number
  rowsPerPage: number
  onPageChange: (page: number) => void
  onRowsPerPageChange: (n: number) => void
  children: ReactNode
}) {
  if (!ready || (loading && count === 0)) {
    return <PageLoading />
  }
  return (
    <>
      {count === 0 ? <EmptyRegister message={emptyMessage} action={emptyAction} /> : children}
      <RegisterPagination
        count={count}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={onPageChange}
        onRowsPerPageChange={onRowsPerPageChange}
      />
    </>
  )
}
