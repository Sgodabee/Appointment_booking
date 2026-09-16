import { useState, useEffect } from 'react';
import { NavLink, Link } from 'react-router-dom';
import { useEmployeeAuth } from '../../context/EmployeeAuthContext';

const navLinks = [
  { to: '/',       label: 'Home',             end: true },
  { to: '/book',   label: 'Book Appointment', end: false },
  { to: '/lookup', label: 'My Appointment',   end: false },
  { to: '/cancel', label: 'Cancel',           end: false },
];

export default function Navbar() {
  const [open,     setOpen]     = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const { employee, logout }    = useEmployeeAuth();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className={`sticky top-0 z-50 transition-all duration-300
        ${scrolled
          ? 'bg-[#001e3a]/95 backdrop-blur-md shadow-lg border-b border-white/10'
          : ''
        }`}
      style={!scrolled ? { background: 'linear-gradient(135deg, #003259 0%, #005A9C 50%, #0070bf 100%)' } : {}}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">

          {/* ── Logo ── */}
          <Link
            to="/"
            className="flex items-center gap-3 group"
            aria-label="Capitec Appointments home"
          >
            <div className="relative">
              <div className="w-9 h-9 rounded-xl bg-white flex items-center justify-center
                              shadow-btn group-hover:shadow-btn-hover transition-shadow duration-200">
                <span className="text-brand-600 font-black text-sm tracking-tight">C</span>
              </div>
              <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-accent-400
                              rounded-full border-2 border-brand-900 animate-pulse-soft" />
            </div>
            <div className="hidden sm:block">
              <p className="text-white font-bold text-base leading-none tracking-tight">
                Capitec
              </p>
              <p className="text-white/60 text-xs font-medium leading-none mt-0.5">
                Branch Appointments
              </p>
            </div>
          </Link>

          {/* ── Desktop nav ── */}
          <nav className="hidden md:flex items-center gap-1" aria-label="Main navigation">
            {navLinks.map(({ to, label, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  `relative px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200
                   ${isActive
                     ? 'text-white'
                     : 'text-white/70 hover:text-white hover:bg-white/10'
                   }`
                }
              >
                {({ isActive }) => (
                  <>
                    {label}
                    {isActive && (
                      <span className="absolute bottom-0 left-1/2 -translate-x-1/2
                                       w-4 h-0.5 bg-accent-400 rounded-full" />
                    )}
                  </>
                )}
              </NavLink>
            ))}

            <div className="w-px h-5 bg-white/20 mx-2" />

            {employee ? (
              <div className="flex items-center gap-2">
                <Link
                  to="/admin"
                  className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium
                             text-white/70 hover:text-white hover:bg-white/10 transition-all duration-200"
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  Admin
                </Link>
                <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/10 text-xs text-white/70">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse-soft" />
                  {employee.fullName.split(' ')[0]}
                  <button
                    onClick={logout}
                    title="Sign out"
                    className="ml-1 text-white/40 hover:text-white/80 transition-colors"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h5a2 2 0 012 2v1" />
                    </svg>
                  </button>
                </div>
              </div>
            ) : null}
          </nav>

          {/* ── Book CTA (desktop) ── */}
          <div className="hidden md:block">
            <Link to="/book" className="btn-primary btn-lg text-sm py-2 px-5">
              Book Now
            </Link>
          </div>

          {/* ── Mobile hamburger ── */}
          <button
            onClick={() => setOpen(!open)}
            className="md:hidden p-2 rounded-xl text-white/80 hover:text-white
                       hover:bg-white/10 transition-colors"
            aria-expanded={open}
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {open
                ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              }
            </svg>
          </button>
        </div>
      </div>

      {/* ── Mobile menu ── */}
      {open && (
        <div className="md:hidden border-t border-white/10 backdrop-blur-md animate-slide-down"
        style={{ background: 'rgba(0,18,40,0.98)' }}>
          <nav className="px-4 py-4 flex flex-col gap-1" aria-label="Mobile navigation">
            {navLinks.map(({ to, label, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 flex items-center justify-between
                   ${isActive
                     ? 'bg-white/15 text-white'
                     : 'text-white/70 hover:bg-white/10 hover:text-white'
                   }`
                }
              >
                {({ isActive }) => (
                  <>
                    {label}
                    {isActive && (
                      <span className="w-1.5 h-1.5 rounded-full bg-accent-400" />
                    )}
                  </>
                )}
              </NavLink>
            ))}

            <div className="h-px bg-white/10 my-2" />

            {employee ? (
              <>
                <Link
                  to="/admin"
                  onClick={() => setOpen(false)}
                  className="px-4 py-3 rounded-xl text-sm font-medium text-white/70
                             hover:bg-white/10 hover:text-white transition-all duration-200
                             flex items-center justify-between"
                >
                  Admin Dashboard
                  <span className="flex items-center gap-1.5 text-xs text-white/40">
                    <span className="w-1.5 h-1.5 rounded-full bg-green-400" />
                    {employee.fullName.split(' ')[0]}
                  </span>
                </Link>
                <button
                  onClick={() => { logout(); setOpen(false); }}
                  className="px-4 py-3 rounded-xl text-sm font-medium text-red-400
                             hover:bg-red-500/10 hover:text-red-300 transition-all duration-200
                             flex items-center gap-2 w-full text-left"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h5a2 2 0 012 2v1" />
                  </svg>
                  Sign Out
                </button>
              </>
            ) : null}

            <Link
              to="/book"
              onClick={() => setOpen(false)}
              className="btn-primary mt-2 w-full justify-center"
            >
              Book an Appointment
            </Link>
          </nav>
        </div>
      )}
    </header>
  );
}
