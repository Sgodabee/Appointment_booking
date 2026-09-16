const variants = {
  error:   'bg-red-50 border-red-200 text-red-800',
  success: 'bg-green-50 border-green-200 text-green-800',
  warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
  info:    'bg-blue-50 border-blue-200 text-blue-800',
};

const icons = {
  error:   '✕',
  success: '✓',
  warning: '⚠',
  info:    'ℹ',
};

export default function Alert({ variant = 'info', title, children, className = '' }) {
  return (
    <div
      role="alert"
      className={`flex gap-3 p-4 rounded-lg border text-sm ${variants[variant]} ${className}`}
    >
      <span className="text-base leading-5 font-bold flex-shrink-0">{icons[variant]}</span>
      <div>
        {title && <p className="font-semibold mb-0.5">{title}</p>}
        <div>{children}</div>
      </div>
    </div>
  );
}
