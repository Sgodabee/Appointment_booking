import { Link } from 'react-router-dom';

const services = [
  { icon: '🏦', label: 'Account Opening',      color: 'hover:border-blue-300   hover:bg-blue-50'   },
  { icon: '💳', label: 'Card Services',         color: 'hover:border-green-300  hover:bg-green-50'  },
  { icon: '💼', label: 'Loan Application',      color: 'hover:border-purple-300 hover:bg-purple-50' },
  { icon: '📋', label: 'Document Submission',   color: 'hover:border-orange-300 hover:bg-orange-50' },
  { icon: '💡', label: 'General Enquiry',       color: 'hover:border-yellow-300 hover:bg-yellow-50' },
  { icon: '📈', label: 'Investment Advice',     color: 'hover:border-teal-300   hover:bg-teal-50'   },
];

const steps = [
  {
    step: '01',
    title: 'Choose a Branch',
    desc: 'Select the Capitec branch nearest to you from our list of locations.',
    icon: '📍',
  },
  {
    step: '02',
    title: 'Pick a Service & Time',
    desc: 'Select what you need help with and choose a date and time that suits you.',
    icon: '🗓️',
  },
  {
    step: '03',
    title: 'Confirm & Walk In',
    desc: 'Receive your email confirmation and arrive at your scheduled time.',
    icon: '✅',
  },
];

export default function HomePage() {
  return (
    <div className="animate-fade-in">

      {/* ─── Hero ──────────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden"
        style={{ background: 'linear-gradient(135deg, #003259 0%, #005A9C 50%, #0070bf 100%)' }}>

        {/* Decorative background circles */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full
                          bg-white/5 blur-3xl" />
          <div className="absolute top-1/2 -left-20 w-72 h-72 rounded-full
                          bg-white/5 blur-2xl" />
          <div className="absolute bottom-0 right-1/4 w-48 h-48 rounded-full
                          bg-accent-400/10 blur-2xl" />
        </div>

        <div className="relative max-w-6xl mx-auto px-4 sm:px-6 py-20 sm:py-28">
          <div className="max-w-2xl">

            <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full
              text-xs font-bold tracking-widest uppercase mb-6
              bg-white/10 text-white/80 border border-white/20">
              🗓️ Branch Appointment System
            </span>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white
                           leading-[1.1] tracking-tight mb-6">
              Book Your Branch Visit
              <span className="block text-accent-400 mt-1">Quickly & Securely</span>
            </h1>

            <p className="text-lg text-white/75 mb-10 leading-relaxed max-w-xl">
              Skip the queues. Choose your preferred branch, service, date and time.
              Get an instant email confirmation and walk in when you're ready.
            </p>

            <div className="flex flex-col sm:flex-row gap-4">
              <Link
                to="/book"
                className="btn-primary btn-lg text-base shadow-btn-hover"
              >
                Book an Appointment
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M17 8l4 4m0 0l-4 4m4-4H3" />
                </svg>
              </Link>
              <Link
                to="/lookup"
                className="btn btn-lg text-base bg-white/10 text-white border border-white/25
                           hover:bg-white/20 hover:-translate-y-0.5 backdrop-blur-sm"
              >
                Look Up My Appointment
              </Link>

            </div>
          </div>
        </div>

        {/* Wave divider */}
        <div className="relative">
          <svg viewBox="0 0 1440 80" className="w-full block" preserveAspectRatio="none" height="80">
            <path d="M0,40 C240,80 480,0 720,40 C960,80 1200,0 1440,40 L1440,80 L0,80 Z"
              fill="#f9fafb" />
          </svg>
        </div>
      </section>

      {/* ─── Services ──────────────────────────────────────────────────────── */}
      <section className="bg-gradient-to-b from-gray-50 to-white">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20">
          <div className="text-center mb-12">
            <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full
              text-xs font-bold tracking-widest uppercase
              bg-brand-50 text-brand-600 border border-brand-100 mb-4">
              What We Offer
            </span>
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mt-3">
              Available Services
            </h2>
            <p className="text-gray-500 mt-3">
              Book an appointment for any of the following services
            </p>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
            {services.map(({ icon, label, color }) => (
              <Link
                key={label}
                to="/book"
                className={`card p-5 flex flex-col items-center gap-3 text-center
                            transition-all duration-200 hover:-translate-y-1
                            hover:shadow-card-hover border border-gray-100 ${color}`}
              >
                <span className="text-3xl">{icon}</span>
                <span className="text-xs font-semibold text-gray-700 leading-tight">{label}</span>
              </Link>
            ))}
          </div>

          <div className="text-center mt-12">
            <Link to="/book" className="btn-primary btn-lg">
              Book an Appointment — It's Free
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M17 8l4 4m0 0l-4 4m4-4H3" />
              </svg>
            </Link>
          </div>
        </div>
      </section>

      {/* ─── How it works ──────────────────────────────────────────────────── */}
      <section className="max-w-5xl mx-auto px-4 sm:px-6 py-20">
        <div className="text-center mb-14">
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full
            text-xs font-bold tracking-widest uppercase
            bg-brand-50 text-brand-600 border border-brand-100 mb-4">
            Simple Process
          </span>
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mt-3">
            How It Works
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 relative">
          {/* Connecting line (desktop) */}
          <div className="hidden sm:block absolute top-10 left-1/6 right-1/6 h-0.5
                          bg-gradient-to-r from-transparent via-brand-200 to-transparent" />

          {steps.map(({ step, title, desc, icon }, i) => (
            <div key={step} className="flex flex-col items-center text-center gap-4 relative">
              <div className="relative animate-float" style={{ animationDelay: `${i * 0.4}s` }}>
                <div className="w-20 h-20 rounded-3xl flex items-center justify-center text-3xl shadow-md"
                  style={{ background: 'linear-gradient(135deg, #003259 0%, #005A9C 50%, #0070bf 100%)' }}>
                  {icon}
                </div>
                <div className="absolute -top-2 -right-2 w-7 h-7 rounded-full bg-white
                                border-2 border-brand-500 flex items-center justify-center
                                text-xs font-black text-brand-600 shadow-sm">
                  {step.replace('0', '')}
                </div>
              </div>
              <h3 className="font-bold text-gray-900 text-lg">{title}</h3>
              <p className="text-sm text-gray-500 leading-relaxed max-w-xs">{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ─── CTA banner ────────────────────────────────────────────────────── */}
      <section className="mx-4 sm:mx-6 lg:mx-auto max-w-5xl mb-20">
        <div className="rounded-3xl p-10 sm:p-14 text-center overflow-hidden relative"
          style={{ background: 'linear-gradient(135deg, #003259 0%, #005A9C 50%, #0070bf 100%)' }}>
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute -top-16 -right-16 w-64 h-64 rounded-full bg-white/5 blur-2xl" />
            <div className="absolute -bottom-16 -left-16 w-64 h-64 rounded-full bg-white/5 blur-2xl" />
          </div>
          <div className="relative">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4">
              Ready to skip the queue?
            </h2>
            <p className="text-white/75 text-lg mb-8 max-w-md mx-auto">
              Book your Capitec branch appointment in under 2 minutes.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link
                to="/book"
                className="btn btn-lg bg-white text-brand-700 font-bold
                           hover:bg-brand-50 shadow-btn hover:shadow-btn-hover
                           hover:-translate-y-0.5 transition-all"
              >
                Book Now — It's Free
              </Link>
              <Link
                to="/lookup"
                className="btn btn-lg bg-white/10 text-white border border-white/25
                           hover:bg-white/20 hover:-translate-y-0.5 backdrop-blur-sm"
              >
                Find My Booking
              </Link>
            </div>
          </div>
        </div>
      </section>

    </div>
  );
}
