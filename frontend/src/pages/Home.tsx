import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from "@/components/ui/use-toast"
import { createShortUrl } from "@/services/api"
import { copyToClipboard } from "@/lib/utils"
import { Link2, Copy, Check, QrCode, Clock, ExternalLink } from "lucide-react"
import type { CreateUrlResponse } from "@/types"

export default function Home() {
  const [url, setUrl] = useState("")
  const [alias, setAlias] = useState("")
  const [expiration, setExpiration] = useState("")
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<CreateUrlResponse | null>(null)
  const [copied, setCopied] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setResult(null)

    try {
      const response = await createShortUrl({
        originalUrl: url,
        customAlias: alias || undefined,
        expirationMinutes: expiration ? parseInt(expiration) : undefined,
      })
      setResult(response)
      toast({ title: "URL criada!", description: "Sua URL curta foi gerada com sucesso.", variant: "success" })
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Erro ao criar URL"
      toast({ title: "Erro", description: message, variant: "destructive" })
    } finally {
      setLoading(false)
    }
  }

  async function handleCopy() {
    if (!result) return
    await copyToClipboard(result.shortUrl)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Encurtador de URLs</h1>
        <p className="text-muted-foreground">
          Crie links curtos, acompanhe métricas e gere QR codes.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Link2 className="h-5 w-5" />
            Nova URL
          </CardTitle>
          <CardDescription>Cole a URL que deseja encurtar</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="url">URL Original *</Label>
              <Input
                id="url"
                type="url"
                placeholder="https://exemplo.com/caminho/muito/longo"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="alias">Alias (opcional)</Label>
                <Input
                  id="alias"
                  placeholder="meu-link"
                  value={alias}
                  onChange={(e) => setAlias(e.target.value)}
                  minLength={3}
                  maxLength={32}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="expiration">Expiração em min (opcional)</Label>
                <Input
                  id="expiration"
                  type="number"
                  placeholder="1440"
                  min={1}
                  max={43200}
                  value={expiration}
                  onChange={(e) => setExpiration(e.target.value)}
                />
              </div>
            </div>

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Encurtando..." : "Encurtar URL"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {result && (
        <Card className="border-primary/50">
          <CardHeader>
            <CardTitle className="text-lg">URL Criada</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-2 rounded-lg bg-muted p-3">
              <code className="flex-1 text-sm font-mono break-all">{result.shortUrl}</code>
              <Button variant="ghost" size="icon" onClick={handleCopy}>
                {copied ? <Check className="h-4 w-4 text-green-500" /> : <Copy className="h-4 w-4" />}
              </Button>
            </div>

            <div className="grid grid-cols-1 gap-2 text-sm text-muted-foreground">
              <div className="flex items-center gap-2">
                <ExternalLink className="h-4 w-4" />
                <span className="truncate">{result.originalUrl}</span>
              </div>
              {result.expiresAt && (
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  <span>Expira em: {new Date(result.expiresAt).toLocaleString("pt-BR")}</span>
                </div>
              )}
            </div>

            <div className="flex gap-2">
              <Button variant="outline" size="sm" className="gap-2" asChild>
                <a href={`/api/urls/${result.shortCode}/qr`} target="_blank" rel="noreferrer">
                  <QrCode className="h-4 w-4" />
                  QR Code
                </a>
              </Button>
              <Button variant="outline" size="sm" className="gap-2" asChild>
                <a href={`/dashboard?stats=${result.shortCode}`}>
                  <ExternalLink className="h-4 w-4" />
                  Ver Estatísticas
                </a>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
