const AUTH_TOKEN_KEY = 'authToken';

let conversationId = null;
let sending = false;
let authMode = 'login';

const authScreen = document.getElementById('auth-screen');
const appScreen = document.getElementById('app');
const authForm = document.getElementById('auth-form');
const authUsername = document.getElementById('auth-username');
const authPassword = document.getElementById('auth-password');
const authSubmit = document.getElementById('auth-submit');
const authError = document.getElementById('auth-error');
const authToggleLink = document.getElementById('auth-toggle-link');
const logoutButton = document.getElementById('logout-button');

const chatLog = document.getElementById('chat-log');
const form = document.getElementById('chat-form');
const messageInput = document.getElementById('message-input');
const imageInput = document.getElementById('image-input');
const submitButton = form.querySelector('button[type="submit"]');
const attachmentPreview = document.getElementById('attachment-preview');
const attachmentThumb = document.getElementById('attachment-thumb');
const attachmentName = document.getElementById('attachment-name');
const attachmentRemove = document.getElementById('attachment-remove');

function getToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

function setToken(token) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
}

function clearToken() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
}

function showAuthScreen() {
    authScreen.hidden = false;
    appScreen.hidden = true;
}

function showApp() {
    authScreen.hidden = true;
    appScreen.hidden = false;
}

authToggleLink.addEventListener('click', (event) => {
    event.preventDefault();
    authMode = authMode === 'login' ? 'register' : 'login';
    authSubmit.textContent = authMode === 'login' ? 'Accedi' : 'Registrati';
    authToggleLink.textContent = authMode === 'login'
        ? 'Non hai un account? Registrati'
        : 'Hai già un account? Accedi';
    authError.hidden = true;
});

authForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    authError.hidden = true;

    const username = authUsername.value.trim();
    const password = authPassword.value;
    if (!username || !password) {
        return;
    }

    const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/register';
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'Errore di autenticazione');
        }
        setToken(data.token);
        authForm.reset();
        showApp();
    } catch (error) {
        authError.textContent = error.message;
        authError.hidden = false;
    }
});

logoutButton.addEventListener('click', () => {
    clearToken();
    conversationId = null;
    chatLog.innerHTML = '';
    showAuthScreen();
});

if (getToken()) {
    showApp();
} else {
    showAuthScreen();
}

imageInput.addEventListener('change', () => {
    const file = imageInput.files[0];
    if (!file) {
        hideAttachmentPreview();
        return;
    }
    attachmentThumb.src = URL.createObjectURL(file);
    attachmentName.textContent = file.name;
    attachmentPreview.hidden = false;
});

attachmentRemove.addEventListener('click', () => {
    imageInput.value = '';
    hideAttachmentPreview();
});

function hideAttachmentPreview() {
    attachmentPreview.hidden = true;
    attachmentThumb.src = '';
    attachmentName.textContent = '';
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (sending) {
        return;
    }

    const message = messageInput.value.trim();
    const file = imageInput.files[0];
    if (!message && !file) {
        return;
    }

    appendMessage('user', message || '(foto allegata)', file);
    messageInput.value = '';
    imageInput.value = '';
    hideAttachmentPreview();

    const assistantBubble = appendMessage('assistant', 'Sto pensando...');
    setSending(true);

    try {
        const data = file
            ? await sendImageMessage(message, file)
            : await sendTextMessage(message);
        conversationId = data.conversationId;
        updateBubble(assistantBubble, formatReply(data.reply), false);
    } catch (error) {
        updateBubble(assistantBubble, 'Errore: il servizio non ha risposto. Riprova tra poco.', true);
    } finally {
        setSending(false);
    }
});

async function sendTextMessage(message) {
    const response = await fetch('/api/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify({ conversationId, message })
    });
    if (response.status === 401) {
        clearToken();
        showAuthScreen();
        throw new Error('Sessione scaduta, effettua di nuovo il login');
    }
    if (!response.ok) {
        throw new Error('Request failed: ' + response.status);
    }
    return response.json();
}

async function sendImageMessage(message, file) {
    const formData = new FormData();
    formData.append('image', file);
    if (message) {
        formData.append('message', message);
    }
    if (conversationId !== null) {
        formData.append('conversationId', conversationId);
    }
    const response = await fetch('/api/chat/image', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getToken() },
        body: formData
    });
    if (response.status === 401) {
        clearToken();
        showAuthScreen();
        throw new Error('Sessione scaduta, effettua di nuovo il login');
    }
    if (!response.ok) {
        throw new Error('Request failed: ' + response.status);
    }
    return response.json();
}

function appendMessage(role, text, file) {
    const wrapper = document.createElement('div');
    wrapper.className = 'message ' + role;

    const bubble = document.createElement('div');
    bubble.className = 'bubble';

    if (file) {
        const img = document.createElement('img');
        img.src = URL.createObjectURL(file);
        img.className = 'attached-image';
        bubble.appendChild(img);
    }

    const content = document.createElement('div');
    content.className = 'bubble-content';
    content.textContent = text;
    bubble.appendChild(content);

    wrapper.appendChild(bubble);
    chatLog.appendChild(wrapper);
    chatLog.scrollTop = chatLog.scrollHeight;
    return wrapper;
}

function updateBubble(wrapper, html, isError) {
    const bubble = wrapper.querySelector('.bubble');
    bubble.classList.toggle('error', isError);
    const content = wrapper.querySelector('.bubble-content');
    content.innerHTML = html;
    chatLog.scrollTop = chatLog.scrollHeight;
}

function setSending(value) {
    sending = value;
    submitButton.disabled = value;
}

function formatReply(text) {
    const escaped = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    return escaped
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>');
}
