const labels = {
  confirmed: 'Confirmed',
  pending:   'Pending',
  cancelled: 'Cancelled',
  completed: 'Completed',
  no_show:   'No Show',
};

export default function Badge({ status }) {
  return (
    <span className={`badge badge-${status}`}>
      {labels[status] ?? status}
    </span>
  );
}
