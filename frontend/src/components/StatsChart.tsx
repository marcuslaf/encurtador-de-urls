import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"
import type { DailyAccess } from "@/types"
import { useTheme } from "@/contexts/ThemeContext"

interface StatsChartProps {
  data: DailyAccess[]
}

export default function StatsChart({ data }: StatsChartProps) {
  const { theme } = useTheme()

  if (data.length === 0) {
    return <p className="text-center py-8 text-muted-foreground">Nenhum dado de acesso disponível.</p>
  }

  const chartData = data.map((d) => ({
    date: new Date(d.date).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" }),
    acessos: d.count,
  }))

  const textColor = theme === "dark" ? "#a1a1aa" : "#71717a"
  const gridColor = theme === "dark" ? "#27272a" : "#e4e4e7"

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
          <XAxis dataKey="date" tick={{ fontSize: 12, fill: textColor }} />
          <YAxis tick={{ fontSize: 12, fill: textColor }} allowDecimals={false} />
          <Tooltip
            contentStyle={{
              backgroundColor: theme === "dark" ? "#18181b" : "#ffffff",
              border: `1px solid ${gridColor}`,
              borderRadius: "8px",
              fontSize: "13px",
            }}
          />
          <Bar dataKey="acessos" fill="hsl(221.2, 83.2%, 53.3%)" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
