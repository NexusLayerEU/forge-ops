export default function DashboardPage() {
  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold text-text-primary mb-6">Dashboard</h1>
      <div className="grid grid-cols-4 gap-4 mb-6">
        {['Total Nodes', 'Active Runs', 'Drifted Nodes', 'Forges'].map(label => (
          <div key={label} className="card">
            <p className="text-text-secondary text-xs mb-1">{label}</p>
            <p className="text-2xl font-mono font-semibold text-text-primary">—</p>
          </div>
        ))}
      </div>
      <p className="text-text-secondary text-sm">Dashboard — coming in Phase 6</p>
    </div>
  )
}
