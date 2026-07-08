const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

function buildUrl(path) {
    return `${apiBaseUrl}${path}`;
}

export async function apiFetch(path, options = {}) {
    const { headers: customHeaders = {}, ...restOptions } = options;

    const response = await fetch(buildUrl(path), {
        ...restOptions,
        headers: {
            "Content-Type": "application/json",
            ...customHeaders
        }
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `${response.status} ${response.statusText}`);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}
