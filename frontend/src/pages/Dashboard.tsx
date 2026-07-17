import { useEffect, useState, useCallback } from "react"
import { useSearchParams } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { toast } from "@/components/ui/use-toast"
import { listUrls, deleteUrl, getUrlStats, getRedirectUrl } from "@/services/api"
import { copyToClipboard, formatDate } from "@/lib/utils"
import {
  Search,
  Trash2,
  Copy,
  Check,
  ChevronLeft,
  ChevronRight,
  Link2,
  BarChart3,
  Eye,
  QrCode,
  ExternalLink,
} from "lucide-react"
import type { CreateUrlResponse, UrlStatsResponse } from "@/types"
import StatsChart from "@/components/StatsChart"

export default function Dashboard() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [urls, setUrls] = useState<CreateUrlResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState("")
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CreateUrlResponse | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [statsCode, setStatsCode] = useState<string | null>(searchParams.get("stats"))
  const [stats, setStats] = useState<UrlStatsResponse | null>(null)
  const [loadingStats, setLoadingStats] = useState(false)

  const loadUrls = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listUrls(page, 20)
      setUrls(data.content)
      setTotalPages(data.totalPages)
    } catch {
      toast({ title: "Erro", description: "Falha ao carregar URLs", variant: "destructive" })
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { loadUrls() }, [loadUrls])

  useEffect(() => {
    const code = searchParams.get("stats")
    if (code) {
      setStatsCode(code)
    }
  }, [searchParams])

  useEffect(() => {
    if (!statsCode) { setStats(null); return }
    setLoadingStats(true)
    getUrlStats(statsCode)
      .then(setStats)
      .catch(() => toast({ title: "Erro", description: "Falha ao carregar estatísticas", variant: "destructive" }))
      .finally(() => setLoadingStats(false))
  }, [statsCode])

  async function handleCopy(url: CreateUrlResponse) {
    await copyToClipboard(url.shortUrl)
    setCopiedId(url.shortCode)
    setTimeout(() => setCopiedId(null), 2000)
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteUrl(deleteTarget.shortCode)
      toast({ title: "URL deletada", description: "A URL foi desativada com sucesso.", variant: "success" })
      setDeleteTarget(null)
      loadUrls()
    } catch {
      toast({ title: "Erro", description: "Falha ao deletar URL", variant: "destructive" })
    } finally {
      setDeleting(false)
    }
  }

  const filtered = urls.filter(
    (u) =>
      u.originalUrl.toLowerCase().includes(search.toLowerCase()) ||
      u.shortCode.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-muted-foreground">Gerencie suas URLs encurtadas.</p>
        </div>
        <Button variant="outline" size="sm" onClick={loadUrls} className="gap-2">
          <Eye className="h-4 w-4" />
          Atualizar
        </Button>
      </div>

      {stats && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2 text-lg">
                <BarChart3 className="h-5 w-5" />
                Estatísticas — {stats.shortCode}
              </CardTitle>
              <CardDescription>{stats.totalAccesses} acessos no total</CardDescription>
            </div>
            <Button variant="ghost" size="sm" onClick={() => { setStatsCode(null); setSearchParams({}) }}>
              Fechar
            </Button>
          </CardHeader>
          <CardContent>
            <StatsChart data={stats.dailyAccesses} />
          </CardContent>
        </Card>
      )}

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Buscar por URL ou código..."
          className="pl-9"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="text-center py-12 text-muted-foreground">Carregando...</div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">Nenhuma URL encontrada.</div>
      ) : (
        <div className="space-y-3">
          {filtered.map((url) => (
            <Card key={url.shortCode}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <Link2 className="h-4 w-4 shrink-0 text-primary" />
                      <code className="text-sm font-mono font-semibold text-primary">
                        {url.shortUrl}
                      </code>
                      <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => handleCopy(url)}>
                        {copiedId === url.shortCode ? (
                          <Check className="h-3.5 w-3.5 text-green-500" />
                        ) : (
                          <Copy className="h-3.5 w-3.5" />
                        )}
                      </Button>
                    </div>
                    <p className="text-sm text-muted-foreground truncate">{url.originalUrl}</p>
                    <p className="text-xs text-muted-foreground">
                      Criada em {formatDate(url.createdAt)}
                      {url.expiresAt && ` · Expira em ${formatDate(url.expiresAt)}`}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => { setStatsCode(url.shortCode); setSearchParams({ stats: url.shortCode }) }}
                    >
                      <BarChart3 className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                      <a href={`/api/urls/${url.shortCode}/qr`} target="_blank" rel="noreferrer">
                        <QrCode className="h-4 w-4" />
                      </a>
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                      <a href={getRedirectUrl(url.shortCode)} target="_blank" rel="noreferrer">
                        <ExternalLink className="h-4 w-4" />
                      </a>
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive hover:text-destructive"
                      onClick={() => setDeleteTarget(url)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm text-muted-foreground">
            Página {page + 1} de {totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirmar exclusão</DialogTitle>
            <DialogDescription>
              Tem certeza que deseja desativar a URL{" "}
              <code className="font-mono">{deleteTarget?.shortCode}</code>?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Cancelar
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? "Deletando..." : "Deletar"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {loadingStats && (
        <div className="text-center py-4 text-muted-foreground text-sm">Carregando estatísticas...</div>
      )}
    </div>
  )
}
