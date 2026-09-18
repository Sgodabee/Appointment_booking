import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { listAppointments, updateAppointmentStatus } from '../api/appointments';
import { useBranches } from '../hooks/useBranches';
import { formatDate, formatTime, formatServiceType } from '../utils/formatters';
import { useEmployeeAuth } from '../context/EmployeeAuthContext';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import Alert from '../components/ui/Alert';

const STATUS_OPTIONS = ['', 'PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW', 'EXPIRED'];
const STATUS_LABELS  = {
  '':          'All Statuses',
  PENDING:     'Pending',
  CONFIRMED:   'Confirmed',
  CANCELLED:   'Cancelled',
  COMPLETED:   'Completed',
  NO_SHOW:     'No Show',
  EXPIRED:     'Expired',
};

// These statuses are final — no further changes are allowed
const TERMINAL_STATUSES = new Set(['COMPLETED', 'CANCELLED', 'EXPIRED']);

const STATUS_COLORS = {
  CONFIRMED:  'bg-green-100 text-green-800 border-green-200',
  PENDING:    'bg-yellow-100 text-yellow-800 border-yellow-200',
  CANCELLED:  'bg-red-100 text-red-800 border-red-200',
  COMPLETED:  'bg-blue-100 text-blue-800 border-blue-200',
  NO_SHOW:    'bg-gray-100 text-gray-600 border-gray-200',
  EXPIRED:    'bg-orange-100 text-orange-700 border-orange-200',
};

export default function AdminPage() {
  const [appointments, setAppointments] = useState([]);
  const [pagination, setPagination]     = useState({ total: 0, page: 1, totalPages: 1 });
  const [loading, setLoading]           = useState(true);
  const [error, setError]               = useState(null);
  const [updatingId, setUpdatingId]     = useState(null);
  const [selectedAppt, setSelectedAppt] = useState(null); // notes drawer
  const [search, setSearch]             = useState('');

  const [filters, setFilters] = useState({
    status: '', branchId: '', date: '', page: 1, size: 15,
  });

  const { employee, logout } = useEmployeeAuth();
  const navigate = useNavigate();

  const { branches } = useBranches();
  const branchMap = Object.fromEntries(branches.map((b) => [b.id, b.name]));

  // ─── Fetch ────────────────────────────────────────────────────────────────
  const fetchAppointments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = Object.fromEntries(
        Object.entries(filters).filter(([, v]) => v !== '' && v !== null)
      );
      const res = await listAppointments(params);
      // res.data is now PagedResponse<AppointmentResponse>: { items, total, page, size, totalPages }
      setAppointments(res.data?.items || []);
      setPagination({
        total:      res.data?.total      ?? 0,
        page:       res.data?.page       ?? 1,
        totalPages: res.data?.totalPages ?? 1,
      });
    } catch (err) {
      setError(err.message || 'Failed to load appointments.');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { fetchAppointments(); }, [fetchAppointments]);

  // ─── Derived stats ────────────────────────────────────────────────────────
  const stats = appointments.reduce((acc, a) => {
    const key = a.status?.toUpperCase();
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});

  // ─── Client-side search filter ────────────────────────────────────────────
  const filtered = search.trim()
    ? appointments.filter((a) => {
        const q = search.toLowerCase();
        return (
          a.customerName?.toLowerCase().includes(q) ||
          a.customerEmail?.toLowerCase().includes(q) ||
          a.referenceNumber?.toLowerCase().includes(q) ||
          a.customerPhone?.toLowerCase().includes(q)
        );
      })
    : appointments;

  // ─── Handlers ─────────────────────────────────────────────────────────────
  const handleFilterChange = (key, value) =>
    setFilters((prev) => ({ ...prev, [key]: value, page: 1 }));

  const clearFilters = () => {
    setFilters({ status: '', branchId: '', date: '', page: 1, size: 15 });
    setSearch('');
  };

  const handleStatusUpdate = async (id, newStatus) => {
    setUpdatingId(id);
    try {
      const res = await updateAppointmentStatus(id, newStatus);
      setAppointments((prev) =>
        prev.map((a) => (a.id === id ? { ...a, status: res.data.status } : a))
      );
      if (selectedAppt?.id === id) {
        setSelectedAppt((prev) => ({ ...prev, status: res.data.status }));
      }
      toast.success(`Status updated to ${STATUS_LABELS[newStatus] ?? newStatus}`);
    } catch (err) {
      toast.error(err.message || 'Failed to update status.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-10 animate-fade-in">

      {/* ─── Header ─────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">
            {pagination.total ?? 0} total appointments
          </p>
        </div>
        <div className="flex items-center gap-3 self-start sm:self-auto">
          {employee && (
            <div className="hidden sm:flex items-center gap-2 text-sm text-gray-500 bg-gray-100
                            rounded-xl px-3 py-2">
              <span className="w-2 h-2 rounded-full bg-green-400" />
              <span className="font-medium text-gray-700">{employee.fullName}</span>
              <span className="text-xs text-gray-400 bg-gray-200 rounded-full px-2 py-0.5">
                {employee.role}
              </span>
            </div>
          )}
          <button onClick={fetchAppointments} className="btn-secondary gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
          <button
            onClick={() => { logout(); navigate('/admin/login'); }}
            className="btn-secondary gap-2 text-red-600 hover:text-red-700 hover:border-red-200"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h5a2 2 0 012 2v1" />
            </svg>
            Sign Out
          </button>
        </div>
      </div>

      {/* ─── Stats bar ──────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-6">
        {[
          { label: 'Confirmed',  key: 'CONFIRMED',  icon: '✅', color: 'border-green-200 bg-green-50' },
          { label: 'Pending',    key: 'PENDING',    icon: '⏳', color: 'border-yellow-200 bg-yellow-50' },
          { label: 'Cancelled',  key: 'CANCELLED',  icon: '❌', color: 'border-red-200 bg-red-50' },
          { label: 'Completed',  key: 'COMPLETED',  icon: '🏁', color: 'border-blue-200 bg-blue-50' },
          { label: 'No Show',    key: 'NO_SHOW',    icon: '👻', color: 'border-gray-200 bg-gray-50'   },
          { label: 'Expired',    key: 'EXPIRED',    icon: '⏰', color: 'border-orange-200 bg-orange-50' },
        ].map(({ label, key, icon, color }) => (
          <button
            key={key}
            onClick={() => handleFilterChange('status', filters.status === key ? '' : key)}
            className={`card border p-3 text-center transition-all hover:shadow-md ${color}
              ${filters.status === key ? 'ring-2 ring-brand-400' : ''}`}
          >
            <div className="text-xl mb-1">{icon}</div>
            <div className="text-xl font-bold text-gray-900">{stats[key] ?? 0}</div>
            <div className="text-xs text-gray-500 font-medium">{label}</div>
          </button>
        ))}
      </div>

      {/* ─── Filters ────────────────────────────────────────────────────── */}
      <div className="card p-4 mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">

          {/* Search */}
          <div className="relative lg:col-span-2">
            <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              placeholder="Search name, email, reference, phone…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="form-input pl-9 text-sm"
            />
          </div>

          {/* Status */}
          <select
            className="form-input text-sm"
            value={filters.status}
            onChange={(e) => handleFilterChange('status', e.target.value)}
            aria-label="Filter by status"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{STATUS_LABELS[s]}</option>
            ))}
          </select>

          {/* Branch */}
          <select
            className="form-input text-sm"
            value={filters.branchId}
            onChange={(e) => handleFilterChange('branchId', e.target.value)}
            aria-label="Filter by branch"
          >
            <option value="">All Branches</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>

          {/* Date */}
          <div className="flex gap-2">
            <input
              type="date"
              className="form-input text-sm flex-1"
              value={filters.date}
              onChange={(e) => handleFilterChange('date', e.target.value)}
              aria-label="Filter by date"
            />
            <button
              className="btn-secondary text-sm px-3 flex-shrink-0"
              onClick={clearFilters}
              title="Clear all filters"
            >
              ✕
            </button>
          </div>

        </div>

        {/* Active filter pills */}
        {(filters.status || filters.branchId || filters.date || search) && (
          <div className="flex flex-wrap gap-2 mt-3 pt-3 border-t border-gray-100">
            {search && (
              <span className="inline-flex items-center gap-1 text-xs bg-brand-50 text-brand-700 border border-brand-200 rounded-full px-2.5 py-1">
                Search: "{search}"
                <button onClick={() => setSearch('')} className="ml-1 hover:text-brand-900">✕</button>
              </span>
            )}
            {filters.status && (
              <span className="inline-flex items-center gap-1 text-xs bg-brand-50 text-brand-700 border border-brand-200 rounded-full px-2.5 py-1">
                {STATUS_LABELS[filters.status]}
                <button onClick={() => handleFilterChange('status', '')} className="ml-1 hover:text-brand-900">✕</button>
              </span>
            )}
            {filters.branchId && (
              <span className="inline-flex items-center gap-1 text-xs bg-brand-50 text-brand-700 border border-brand-200 rounded-full px-2.5 py-1">
                {branchMap[filters.branchId] ?? `Branch #${filters.branchId}`}
                <button onClick={() => handleFilterChange('branchId', '')} className="ml-1 hover:text-brand-900">✕</button>
              </span>
            )}
            {filters.date && (
              <span className="inline-flex items-center gap-1 text-xs bg-brand-50 text-brand-700 border border-brand-200 rounded-full px-2.5 py-1">
                {filters.date}
                <button onClick={() => handleFilterChange('date', '')} className="ml-1 hover:text-brand-900">✕</button>
              </span>
            )}
          </div>
        )}
      </div>

      {/* ─── Error ──────────────────────────────────────────────────────── */}
      {error && <Alert variant="error" className="mb-6">{error}</Alert>}

      {/* ─── Loading ────────────────────────────────────────────────────── */}
      {loading && (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      )}

      {/* ─── Empty ──────────────────────────────────────────────────────── */}
      {!loading && filtered.length === 0 && !error && (
        <div className="text-center py-20 text-gray-400">
          <div className="text-5xl mb-4">📭</div>
          <p className="font-medium">No appointments found</p>
          <p className="text-sm mt-1">Try adjusting your filters</p>
        </div>
      )}

      {/* ─── Main layout: table + detail panel ──────────────────────────── */}
      {!loading && filtered.length > 0 && (
        <div className={`flex gap-5 items-start ${selectedAppt ? 'flex-col lg:flex-row' : ''}`}>

          {/* ── Table ─────────────────────────────────────────────────── */}
          <div className={`min-w-0 ${selectedAppt ? 'lg:flex-1' : 'w-full'}`}>

            {/* Desktop table */}
            <div className="hidden lg:block card overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      {['Reference', 'Customer', 'Service', 'Branch', 'Date & Time', 'Status', 'Actions'].map((h) => (
                        <th key={h} className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {filtered.map((appt) => (
                      <tr
                        key={appt.id}
                        onClick={() => setSelectedAppt(selectedAppt?.id === appt.id ? null : appt)}
                        className={`transition-colors cursor-pointer
                          ${selectedAppt?.id === appt.id
                            ? 'bg-brand-50 border-l-2 border-l-brand-500'
                            : 'hover:bg-gray-50'}`}
                      >
                        <td className="px-4 py-3 font-mono text-brand-600 font-semibold whitespace-nowrap">
                          {appt.referenceNumber}
                        </td>
                        <td className="px-4 py-3">
                          <div className="font-medium text-gray-900">{appt.customerName}</div>
                          <div className="text-gray-400 text-xs">{appt.customerEmail}</div>
                          <div className="text-gray-400 text-xs">{appt.customerPhone}</div>
                        </td>
                        <td className="px-4 py-3 text-gray-700 whitespace-nowrap">
                          {formatServiceType(appt.serviceType)}
                        </td>
                        <td className="px-4 py-3 text-gray-700 whitespace-nowrap">
                          {appt.branchName ?? branchMap[appt.branchId] ?? `Branch #${appt.branchId}`}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <div className="text-gray-800 font-medium">{formatDate(appt.appointmentDate)}</div>
                          <div className="text-gray-400 text-xs">{formatTime(appt.appointmentTime)}</div>
                        </td>
                        <td className="px-4 py-3">
                          <Badge status={appt.status} />
                        </td>
                        <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                          {TERMINAL_STATUSES.has(appt.status?.toUpperCase())
                            ? (
                              <span className="text-xs text-gray-400 italic">Locked</span>
                            ) : (
                              <select
                                className="form-input text-xs py-1.5 w-36"
                                value={appt.status?.toUpperCase()}
                                disabled={updatingId === appt.id}
                                onChange={(e) => handleStatusUpdate(appt.id, e.target.value)}
                                aria-label={`Change status for ${appt.referenceNumber}`}
                              >
                                {STATUS_OPTIONS.slice(1).map((s) => (
                                  <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                                ))}
                              </select>
                            )
                          }
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mobile cards */}
            <div className="lg:hidden space-y-3">
              {filtered.map((appt) => (
                <div
                  key={appt.id}
                  className={`card p-4 cursor-pointer transition-all
                    ${selectedAppt?.id === appt.id ? 'border-brand-400 ring-2 ring-brand-200' : 'hover:shadow-md'}`}
                  onClick={() => setSelectedAppt(selectedAppt?.id === appt.id ? null : appt)}
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <span className="font-mono text-sm font-bold text-brand-600">{appt.referenceNumber}</span>
                    <Badge status={appt.status} />
                  </div>
                  <p className="font-semibold text-gray-900 text-sm">{appt.customerName}</p>
                  <p className="text-gray-400 text-xs mb-1">{appt.customerEmail}</p>
                  <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-gray-600 mb-3">
                    <span><span className="font-medium">Service:</span> {formatServiceType(appt.serviceType)}</span>
                    <span><span className="font-medium">Branch:</span> {appt.branchName ?? branchMap[appt.branchId] ?? `Branch #${appt.branchId}`}</span>
                    <span><span className="font-medium">Date:</span> {formatDate(appt.appointmentDate)}</span>
                    <span><span className="font-medium">Time:</span> {formatTime(appt.appointmentTime)}</span>
                  </div>
                  {appt.notes && (
                    <p className="text-xs text-gray-500 italic bg-gray-50 rounded px-2 py-1 mb-3">
                      📝 {appt.notes}
                    </p>
                  )}
                  <div onClick={(e) => e.stopPropagation()}>
                    {TERMINAL_STATUSES.has(appt.status?.toUpperCase())
                      ? (
                        <p className="text-xs text-gray-400 italic text-center py-1">Status is locked</p>
                      ) : (
                        <select
                          className="form-input text-xs py-1.5 w-full"
                          value={appt.status?.toUpperCase()}
                          disabled={updatingId === appt.id}
                          onChange={(e) => handleStatusUpdate(appt.id, e.target.value)}
                        >
                          {STATUS_OPTIONS.slice(1).map((s) => (
                            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                          ))}
                        </select>
                      )
                    }
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            {pagination.totalPages >= 1 && (
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mt-6">

                {/* Left: summary + page size */}
                <div className="flex items-center gap-3 text-sm text-gray-500">
                  <span>
                    Showing{' '}
                    <strong className="text-gray-700">
                      {Math.min((pagination.page - 1) * filters.size + 1, pagination.total)}
                      –
                      {Math.min(pagination.page * filters.size, pagination.total)}
                    </strong>
                    {' '}of{' '}
                    <strong className="text-gray-700">{pagination.total}</strong>
                  </span>
                  <select
                    className="form-input text-xs py-1.5 w-20"
                    value={filters.size}
                    onChange={(e) => handleFilterChange('size', Number(e.target.value))}
                    aria-label="Rows per page"
                  >
                    {[10, 15, 25, 50].map((n) => (
                      <option key={n} value={n}>{n} / page</option>
                    ))}
                  </select>
                </div>

                {/* Right: page buttons */}
                <div className="flex items-center gap-1">

                  {/* First */}
                  <button
                    onClick={() => handleFilterChange('page', 1)}
                    disabled={pagination.page <= 1}
                    className="btn-secondary py-1.5 px-2.5 disabled:opacity-30 text-xs"
                    aria-label="First page"
                  >
                    «
                  </button>

                  {/* Prev */}
                  <button
                    onClick={() => handleFilterChange('page', pagination.page - 1)}
                    disabled={pagination.page <= 1}
                    className="btn-secondary py-1.5 px-3 disabled:opacity-30 text-xs"
                    aria-label="Previous page"
                  >
                    ‹ Prev
                  </button>

                  {/* Numbered pages */}
                  {(() => {
                    const total   = pagination.totalPages;
                    const current = pagination.page;
                    const delta   = 2; // pages shown either side of current
                    const pages   = [];

                    let start = Math.max(1, current - delta);
                    let end   = Math.min(total, current + delta);

                    // Always show at least 5 buttons when possible
                    if (end - start < delta * 2) {
                      if (start === 1) end   = Math.min(total, start + delta * 2);
                      else             start = Math.max(1,     end   - delta * 2);
                    }

                    if (start > 1) {
                      pages.push(1);
                      if (start > 2) pages.push('…');
                    }

                    for (let p = start; p <= end; p++) pages.push(p);

                    if (end < total) {
                      if (end < total - 1) pages.push('…');
                      pages.push(total);
                    }

                    return pages.map((p, i) =>
                      p === '…' ? (
                        <span key={`ellipsis-${i}`} className="px-2 text-gray-400 text-sm select-none">
                          …
                        </span>
                      ) : (
                        <button
                          key={p}
                          onClick={() => handleFilterChange('page', p)}
                          className={`py-1.5 px-3 rounded-xl text-xs font-semibold transition-all border
                            ${p === current
                              ? 'text-white border-brand-500 shadow-sm'
                              : 'btn-secondary'
                            }`}
                          style={p === current
                            ? { background: 'linear-gradient(135deg,#005A9C,#0070bf)' }
                            : {}}
                          aria-label={`Page ${p}`}
                          aria-current={p === current ? 'page' : undefined}
                        >
                          {p}
                        </button>
                      )
                    );
                  })()}

                  {/* Next */}
                  <button
                    onClick={() => handleFilterChange('page', pagination.page + 1)}
                    disabled={pagination.page >= pagination.totalPages}
                    className="btn-secondary py-1.5 px-3 disabled:opacity-30 text-xs"
                    aria-label="Next page"
                  >
                    Next ›
                  </button>

                  {/* Last */}
                  <button
                    onClick={() => handleFilterChange('page', pagination.totalPages)}
                    disabled={pagination.page >= pagination.totalPages}
                    className="btn-secondary py-1.5 px-2.5 disabled:opacity-30 text-xs"
                    aria-label="Last page"
                  >
                    »
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* ── Detail / Notes panel ──────────────────────────────────── */}
          {selectedAppt && (
            <div className="w-full lg:w-80 xl:w-96 flex-shrink-0 card p-5 animate-slide-up sticky top-6">
              {/* Panel header */}
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-gray-900 text-sm">Appointment Details</h3>
                <button
                  onClick={() => setSelectedAppt(null)}
                  className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                  aria-label="Close panel"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Reference + status */}
              <div className="flex items-center justify-between mb-4">
                <span className="font-mono text-brand-600 font-bold text-sm">{selectedAppt.referenceNumber}</span>
                <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${STATUS_COLORS[selectedAppt.status?.toUpperCase()] ?? 'bg-gray-100 text-gray-600'}`}>
                  {STATUS_LABELS[selectedAppt.status?.toUpperCase()] ?? selectedAppt.status}
                </span>
              </div>

              {/* Customer */}
              <div className="bg-gray-50 rounded-xl p-3 mb-4 space-y-1.5">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Customer</p>
                <p className="text-sm font-semibold text-gray-900">{selectedAppt.customerName}</p>
                <a href={`mailto:${selectedAppt.customerEmail}`}
                  className="text-xs text-brand-600 hover:underline block">
                  {selectedAppt.customerEmail}
                </a>
                <a href={`tel:${selectedAppt.customerPhone}`}
                  className="text-xs text-gray-500 hover:text-gray-700 block">
                  {selectedAppt.customerPhone}
                </a>
              </div>

              {/* Appointment info */}
              <div className="space-y-2.5 mb-4 text-sm">
                {[
                  ['🏦 Branch',  selectedAppt.branchName ?? branchMap[selectedAppt.branchId] ?? `Branch #${selectedAppt.branchId}`],
                  ['🛠 Service', formatServiceType(selectedAppt.serviceType)],
                  ['📅 Date',    formatDate(selectedAppt.appointmentDate)],
                  ['🕐 Time',    formatTime(selectedAppt.appointmentTime)],
                ].map(([label, val]) => (
                  <div key={label} className="flex justify-between gap-3">
                    <span className="text-gray-500 flex-shrink-0">{label}</span>
                    <span className="text-gray-900 font-medium text-right">{val}</span>
                  </div>
                ))}
              </div>

              {/* Notes */}
              <div className="mb-4">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                  Customer Note
                </p>
                {selectedAppt.notes
                  ? (
                    <div className="bg-amber-50 border border-amber-200 rounded-xl px-3 py-2.5 text-sm text-gray-800 italic leading-relaxed">
                      "{selectedAppt.notes}"
                    </div>
                  )
                  : (
                    <p className="text-xs text-gray-400 italic">No note left by customer.</p>
                  )
                }
              </div>

              {/* Divider */}
              <div className="border-t border-gray-100 pt-4">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                  Update Status
                </p>
                {TERMINAL_STATUSES.has(selectedAppt.status?.toUpperCase())
                  ? (
                    <div className="flex items-center gap-2 text-xs text-gray-400 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2.5">
                      <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M12 15v2m0 0v2m0-2h2m-2 0H10m2-6V7a4 4 0 10-8 0v4h8z" />
                      </svg>
                      This appointment is {STATUS_LABELS[selectedAppt.status?.toUpperCase()]?.toLowerCase()} and cannot be changed.
                    </div>
                  ) : (
                    <select
                      className="form-input text-sm w-full"
                      value={selectedAppt.status?.toUpperCase()}
                      disabled={updatingId === selectedAppt.id}
                      onChange={(e) => handleStatusUpdate(selectedAppt.id, e.target.value)}
                    >
                      {STATUS_OPTIONS.slice(1).map((s) => (
                        <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                      ))}
                    </select>
                  )
                }
              </div>

              {/* Booked at */}
              {selectedAppt.createdAt && (
                <p className="text-xs text-gray-400 mt-3 text-center">
                  Booked on {new Date(selectedAppt.createdAt).toLocaleDateString('en-ZA', {
                    day: 'numeric', month: 'long', year: 'numeric',
                  })}
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
