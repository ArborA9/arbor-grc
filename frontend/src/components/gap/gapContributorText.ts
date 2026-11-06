/** Doc 04 D4-G4: same allow-list as GapContributorText.java */

export const GAP_COMMENT_HINT = "Letters, numbers, and . , ; : ! ? ' \" - ( ) / only."

export const GAP_COMMENT_MESSAGE =
  "Comments may only include letters, numbers, spaces, and . , ; : ! ? ' \" - ( ) /"

const DISALLOWED = /[^\p{L}\p{N}\s.,;:!?'"()/\u2018\u2019\u201C\u201D\u2013\u2014-]/u

export function isAllowedGapComment(text: string | null | undefined): boolean {
  if (text == null || text.trim() === '') return true
  return !DISALLOWED.test(text)
}

export function gapContributorTextIssue(text: string | null | undefined): string | null {
  return isAllowedGapComment(text) ? null : GAP_COMMENT_MESSAGE
}

export function firstGapCommentIssue(...texts: Array<string | null | undefined>): string | null {
  for (const text of texts) {
    const issue = gapContributorTextIssue(text)
    if (issue) return issue
  }
  return null
}
