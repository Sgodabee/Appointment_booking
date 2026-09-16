import { useState, useEffect } from 'react';
import { getAvailableSlots } from '../api/appointments';

export function useAvailableSlots(branchId, date) {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!branchId || !date) {
      setSlots([]);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    getAvailableSlots(branchId, date)
      .then((res) => { if (!cancelled) setSlots(res.data?.available || []); })
      .catch((err) => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [branchId, date]);

  return { slots, loading, error };
}
