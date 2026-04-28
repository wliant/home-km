import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { FilePreview } from './FileDetailPage'

describe('FilePreview', () => {
  const url = 'https://minio.example.com/bucket/abc'

  it('renders an <img> for image MIME types using thumbnail when available', () => {
    render(
      <FilePreview
        mimeType="image/jpeg"
        filename="photo.jpg"
        downloadUrl={url}
        thumbnailUrl={`${url}_thumb.jpg`}
      />,
    )
    const img = screen.getByRole('img', { name: 'photo.jpg' })
    expect(img).toHaveAttribute('src', `${url}_thumb.jpg`)
  })

  it('falls back to downloadUrl for images without thumbnail', () => {
    render(
      <FilePreview
        mimeType="image/png"
        filename="x.png"
        downloadUrl={url}
        thumbnailUrl={null}
      />,
    )
    expect(screen.getByRole('img')).toHaveAttribute('src', url)
  })

  it('renders an iframe for PDFs with #toolbar=0 fragment', () => {
    const { container } = render(
      <FilePreview
        mimeType="application/pdf"
        filename="report.pdf"
        downloadUrl={url}
        thumbnailUrl={null}
      />,
    )
    const iframe = container.querySelector('iframe')
    expect(iframe).toBeTruthy()
    expect(iframe?.getAttribute('src')).toBe(`${url}#toolbar=0`)
    expect(iframe?.getAttribute('title')).toBe('report.pdf')
  })

  it('renders a <video> for video MIME types', () => {
    const { container } = render(
      <FilePreview
        mimeType="video/mp4"
        filename="clip.mp4"
        downloadUrl={url}
        thumbnailUrl={null}
      />,
    )
    const video = container.querySelector('video')
    expect(video).toBeTruthy()
    expect(video?.querySelector('source')?.getAttribute('src')).toBe(url)
    expect(video?.querySelector('source')?.getAttribute('type')).toBe('video/mp4')
  })

  it('renders an <audio> for audio MIME types', () => {
    const { container } = render(
      <FilePreview
        mimeType="audio/mpeg"
        filename="song.mp3"
        downloadUrl={url}
        thumbnailUrl={null}
      />,
    )
    const audio = container.querySelector('audio')
    expect(audio).toBeTruthy()
    expect(audio?.querySelector('source')?.getAttribute('src')).toBe(url)
  })

  it('renders the MIME placeholder for unsupported types', () => {
    render(
      <FilePreview
        mimeType="application/zip"
        filename="bundle.zip"
        downloadUrl={url}
        thumbnailUrl={null}
      />,
    )
    expect(screen.getByText('application/zip')).toBeInTheDocument()
  })

  it('shows the placeholder when downloadUrl is missing for non-image types', () => {
    render(
      <FilePreview
        mimeType="application/pdf"
        filename="x.pdf"
        downloadUrl={null}
        thumbnailUrl={null}
      />,
    )
    expect(screen.getByText('application/pdf')).toBeInTheDocument()
  })
})
