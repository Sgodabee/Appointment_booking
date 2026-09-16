export default function StepIndicator({ steps, current }) {
  return (
    <nav aria-label="Progress" className="flex items-center gap-0 mb-8">
      {steps.map((label, i) => {
        const num = i + 1;
        const isActive = num === current;
        const isDone   = num < current;
        const circleClass = isDone || isActive ? 'step-active' : 'step-inactive';

        return (
          <div key={label} className="flex items-center">
            <div className="flex flex-col items-center gap-1">
              <div className={`step-circle ${circleClass}`} aria-current={isActive ? 'step' : undefined}>
                {isDone ? (
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                ) : num}
              </div>
              <span className={`text-xs font-medium hidden sm:block ${isActive ? 'text-brand-600' : 'text-gray-400'}`}>
                {label}
              </span>
            </div>
            {i < steps.length - 1 && (
              <div className={`h-0.5 w-10 sm:w-16 mx-1 mb-4 ${isDone ? 'bg-brand-500' : 'bg-gray-200'}`} />
            )}
          </div>
        );
      })}
    </nav>
  );
}
