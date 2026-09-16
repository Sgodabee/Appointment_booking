import { Link } from 'react-router-dom';

const quickLinks = [
  { to: '/book',   label: 'Book Appointment' },
  { to: '/lookup', label: 'My Appointment'   },
  { to: '/cancel', label: 'Cancel Booking'   },
  { to: '/admin',  label: 'Admin Dashboard'  },
];

export default function Footer() {
  return (
    <footer className="bg-brand-900 text-white/70 mt-auto">

      {/* ── Main footer grid ── */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 pt-14 pb-10">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-10">

          {/* Brand column */}
          <div>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center shadow-sm">
                <span className="text-brand-600 font-black text-base">C</span>
              </div>
              <div>
                <p className="text-white font-bold text-base leading-none">Capitec</p>
                <p className="text-white/50 text-xs mt-0.5">Branch Appointments</p>
              </div>
            </div>
            <p className="text-sm leading-relaxed text-white/60 mb-5">
              Skip the queues. Book your branch visit online and walk in when you're ready.
            </p>
            <div className="flex items-center gap-2 text-xs text-white/40">
              <span className="inline-block w-2 h-2 rounded-full bg-green-400 animate-pulse-soft" />
              System operational
            </div>
          </div>

          {/* Quick links */}
          <div>
            <h3 className="text-white font-semibold text-sm uppercase tracking-wider mb-4">
              Quick Links
            </h3>
            <ul className="space-y-2.5">
              {quickLinks.map(({ to, label }) => (
                <li key={to}>
                  <Link
                    to={to}
                    className="text-sm text-white/60 hover:text-white transition-colors duration-150
                               flex items-center gap-2 group"
                  >
                    <svg className="w-3.5 h-3.5 text-brand-400 group-hover:translate-x-0.5 transition-transform"
                      fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

        </div>
      </div>

      {/* ── Bottom bar ── */}
      <div className="border-t border-white/10">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-5 flex flex-col sm:flex-row
                        items-center justify-between gap-3 text-xs text-white/40">
          <p>&copy; {new Date().getFullYear()} Capitec Branch Appointment System. All rights reserved.</p>
          <p>All data is encrypted and stored securely.</p>
        </div>
      </div>

    </footer>
  );
}
