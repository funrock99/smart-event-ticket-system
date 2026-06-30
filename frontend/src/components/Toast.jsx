export default function Toast({ toast }) {
    return (
        <div className={`toast ${toast.type || ""} ${toast.message ? "" : "hidden"}`.trim()}>
            {toast.message}
        </div>
    );
}