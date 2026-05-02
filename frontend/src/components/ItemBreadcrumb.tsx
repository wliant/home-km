import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { folderApi } from '../api'
import { QK } from '../lib/queryKeys'

interface Props {
  folderId: number | null
  itemTitle: string
}

/**
 * Breadcrumb for note/file detail pages: "Home / Folder / Item title".
 * Skips the folder fetch when the item lives at root. The trailing item
 * title is rendered as plain text (current page) per the WAI-ARIA APG
 * pattern; everything else is a Link.
 */
export default function ItemBreadcrumb({ folderId, itemTitle }: Props) {
  const { data: folder } = useQuery({
    queryKey: QK.folder(folderId ?? 0),
    queryFn: () => folderApi.getById(folderId as number),
    enabled: folderId != null,
  })
  const ancestors = folder?.ancestors ?? null

  return (
    <nav aria-label="Breadcrumb" className="text-xs text-gray-500 dark:text-gray-400">
      <ol className="flex flex-wrap items-center gap-1 min-w-0">
        <li>
          <Link to="/" className="hover:text-primary-600 dark:hover:text-primary-400 hover:underline">Home</Link>
        </li>
        {folderId != null && ancestors && ancestors.map(a => (
          <li key={a.id} className="flex items-center gap-1 min-w-0">
            <span aria-hidden="true">/</span>
            <Link
              to={`/folders/${a.id}`}
              className="hover:text-primary-600 dark:hover:text-primary-400 hover:underline truncate max-w-[12rem]"
              title={a.name}
            >
              {a.name}
            </Link>
          </li>
        ))}
        <li className="flex items-center gap-1 min-w-0" aria-current="page">
          <span aria-hidden="true">/</span>
          <span className="text-gray-700 dark:text-gray-300 truncate" title={itemTitle}>
            {itemTitle}
          </span>
        </li>
      </ol>
    </nav>
  )
}
