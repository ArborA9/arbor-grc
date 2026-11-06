import { useEffect, useMemo, useState } from 'react'

export const DEFAULT_PAGE_SIZE = 25

export function usePagedRows<T>(rows: T[], pageSize = DEFAULT_PAGE_SIZE) {
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(pageSize)

  useEffect(() => {
    setPage(0)
  }, [rows, rowsPerPage])

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(rows.length / rowsPerPage) - 1)
    if (page > maxPage) setPage(maxPage)
  }, [page, rows.length, rowsPerPage])

  const paged = useMemo(
    () => rows.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage),
    [rows, page, rowsPerPage],
  )

  return { page, rowsPerPage, paged, setPage, setRowsPerPage, count: rows.length }
}
