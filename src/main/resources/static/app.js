const AUTH_TOKEN_KEY = 'authToken';

let conversationId = null;
let generalConversationId = null;
let conversations = [];
let sending = false;
let authMode = 'login';
let selectedFiles = [];

const authScreen = document.getElementById('auth-screen');
const appScreen = document.getElementById('app');
const authForm = document.getElementById('auth-form');
const authUsername = document.getElementById('auth-username');
const authPassword = document.getElementById('auth-password');
const authSubmit = document.getElementById('auth-submit');
const authError = document.getElementById('auth-error');
const authToggleLink = document.getElementById('auth-toggle-link');
const logoutButton = document.getElementById('logout-button');

const sidebar = document.getElementById('sidebar');
const sidebarToggle = document.getElementById('sidebar-toggle');
const sidebarClose = document.getElementById('sidebar-close');
const conversationList = document.getElementById('conversation-list');
const newPlantButton = document.getElementById('new-plant-button');
const newPlantModal = document.getElementById('new-plant-modal');
const newPlantForm = document.getElementById('new-plant-form');
const newPlantName = document.getElementById('new-plant-name');
const newPlantGroup = document.getElementById('new-plant-group');
const newPlantCancel = document.getElementById('new-plant-cancel');
const conversationTitleEl = document.getElementById('conversation-title');
const conversationSubtitleEl = document.getElementById('conversation-subtitle');
const quotaBadge = document.getElementById('quota-badge');

const accountButton = document.getElementById('account-button');
const accountModal = document.getElementById('account-modal');
const accountClose = document.getElementById('account-close');
const accountUsernameEl = document.getElementById('account-username');
const accountCreatedEl = document.getElementById('account-created');
const accountQuotaEl = document.getElementById('account-quota');

const chatLog = document.getElementById('chat-log');
const form = document.getElementById('chat-form');
const messageInput = document.getElementById('message-input');
const imageInput = document.getElementById('image-input');
const submitButton = form.querySelector('button[type="submit"]');
const attachmentPreview = document.getElementById('attachment-preview');
const attachmentList = document.getElementById('attachment-list');

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
    initConversations();
    updateQuotaBadge();
}

class QuotaExceededError extends Error {}

// Attaches the bearer token to every call and redirects to login on an expired/invalid session.
async function authFetch(url, options = {}) {
    const headers = Object.assign({}, options.headers, { Authorization: 'Bearer ' + getToken() });
    const response = await fetch(url, Object.assign({}, options, { headers }));
    if (response.status === 401) {
        clearToken();
        showAuthScreen();
        throw new Error('Sessione scaduta, effettua di nuovo il login');
    }
    if (response.status === 402) {
        throw new QuotaExceededError('Quota esaurita');
    }
    return response;
}

async function updateQuotaBadge() {
    const status = await (await authFetch('/api/quota')).json();
    if (status.unlimited) {
        quotaBadge.textContent = '✨ Illimitato';
        quotaBadge.classList.add('unlimited');
    } else {
        quotaBadge.textContent = status.used + '/' + status.limit;
        quotaBadge.classList.remove('unlimited');
    }
}

async function startCheckout() {
    const data = await (await authFetch('/api/payment/checkout', { method: 'POST' })).json();
    window.location.href = data.url;
}

function handlePaymentRedirect() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('payment')) {
        history.replaceState({}, '', window.location.pathname);
    }
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
    generalConversationId = null;
    conversations = [];
    chatLog.innerHTML = '';
    showAuthScreen();
});

handlePaymentRedirect();

if (getToken()) {
    showApp();
} else {
    showAuthScreen();
}

// --- Conversation sidebar ---

async function initConversations() {
    const general = await (await authFetch('/api/conversations/general')).json();
    generalConversationId = general.id;
    conversations = await (await authFetch('/api/conversations')).json();
    await selectGeneral();
}

async function selectGeneral() {
    await loadConversation({ id: generalConversationId, title: 'Chat generale', plantationLabel: null, isGeneral: true });
}

async function selectConversation(conversation) {
    await loadConversation(Object.assign({}, conversation, { isGeneral: false }));
}

async function loadConversation(conversation) {
    conversationId = conversation.id;
    conversationTitleEl.textContent = (conversation.isGeneral ? '💬 ' : '🌱 ') + conversation.title;
    conversationSubtitleEl.textContent = conversation.isGeneral
        ? 'Ulivo · Vigneto · Agrumi · Fico · Mandorlo'
        : (conversation.plantationLabel ? 'Gruppo: ' + conversation.plantationLabel : 'Pianta singola');

    chatLog.innerHTML = '';
    renderSidebar();
    sidebar.classList.remove('open');

    const messages = await (await authFetch('/api/conversations/' + conversation.id + '/messages')).json();
    messages.forEach((m) => renderHistoryMessage(m.role, m.content));
}

function renderSidebar() {
    conversationList.innerHTML = '';

    const generalItem = document.createElement('button');
    generalItem.type = 'button';
    generalItem.className = 'conversation-item' + (conversationId === generalConversationId ? ' active' : '');
    generalItem.textContent = '💬 Chat generale';
    generalItem.addEventListener('click', selectGeneral);
    conversationList.appendChild(generalItem);

    conversations.forEach((conversation) => {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'conversation-item' + (conversation.id === conversationId ? ' active' : '');

        const title = document.createElement('span');
        title.className = 'conversation-item-title';
        title.textContent = '🌱 ' + conversation.title;
        item.appendChild(title);

        if (conversation.plantationLabel) {
            const group = document.createElement('span');
            group.className = 'conversation-item-group';
            group.textContent = conversation.plantationLabel;
            item.appendChild(group);
        }

        item.addEventListener('click', () => selectConversation(conversation));
        conversationList.appendChild(item);
    });
}

sidebarToggle.addEventListener('click', () => {
    sidebar.classList.toggle('open');
});

sidebarClose.addEventListener('click', () => {
    sidebar.classList.remove('open');
});

newPlantButton.addEventListener('click', () => {
    newPlantForm.reset();
    newPlantModal.hidden = false;
});

newPlantCancel.addEventListener('click', () => {
    newPlantModal.hidden = true;
});

newPlantForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const name = newPlantName.value.trim();
    if (!name) {
        return;
    }
    const plantationLabel = newPlantGroup.value.trim() || null;

    const response = await authFetch('/api/conversations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: name, plantationLabel })
    });
    const created = await response.json();

    conversations.unshift(created);
    newPlantModal.hidden = true;
    await selectConversation(created);
});

// --- Account panel ---

accountButton.addEventListener('click', async () => {
    const [account, quota] = await Promise.all([
        authFetch('/api/account').then((r) => r.json()),
        authFetch('/api/quota').then((r) => r.json())
    ]);

    accountUsernameEl.textContent = account.username;
    accountCreatedEl.textContent = new Date(account.createdAt).toLocaleDateString('it-IT', {
        year: 'numeric', month: 'long', day: 'numeric'
    });
    accountQuotaEl.textContent = quota.unlimited
        ? 'Illimitato fino alle ' + new Date(quota.windowResetAt).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
        : quota.used + '/' + quota.limit + ' richieste usate';

    accountModal.hidden = false;
});

accountClose.addEventListener('click', () => {
    accountModal.hidden = true;
});

// --- Attachment preview ---

imageInput.addEventListener('change', () => {
    selectedFiles = Array.from(imageInput.files);
    renderAttachmentPreview();
});

function renderAttachmentPreview() {
    attachmentList.innerHTML = '';

    if (selectedFiles.length === 0) {
        attachmentPreview.hidden = true;
        return;
    }

    selectedFiles.forEach((file, index) => {
        const chip = document.createElement('div');
        chip.className = 'attachment-chip';

        const thumb = document.createElement('img');
        thumb.className = 'attachment-thumb';
        thumb.src = URL.createObjectURL(file);
        chip.appendChild(thumb);

        const name = document.createElement('span');
        name.className = 'attachment-name';
        name.textContent = file.name;
        chip.appendChild(name);

        const remove = document.createElement('button');
        remove.type = 'button';
        remove.className = 'attachment-remove';
        remove.title = 'Rimuovi foto';
        remove.textContent = '✕';
        remove.addEventListener('click', () => {
            selectedFiles.splice(index, 1);
            renderAttachmentPreview();
        });
        chip.appendChild(remove);

        attachmentList.appendChild(chip);
    });

    attachmentPreview.hidden = false;
}

// --- Chat ---

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (sending) {
        return;
    }

    const message = messageInput.value.trim();
    const files = selectedFiles;
    if (!message && files.length === 0) {
        return;
    }

    appendMessage('user', message || '(foto allegata)', files);
    messageInput.value = '';
    imageInput.value = '';
    selectedFiles = [];
    renderAttachmentPreview();

    const assistantBubble = appendMessage('assistant', 'Sto pensando...');
    setSending(true);

    try {
        const data = files.length > 0
            ? await sendImageMessage(message, files)
            : await sendTextMessage(message);
        conversationId = data.conversationId;
        updateBubble(assistantBubble, formatReply(data.reply), false);
        updateQuotaBadge();
    } catch (error) {
        if (error instanceof QuotaExceededError) {
            renderQuotaExceededBubble(assistantBubble);
        } else {
            updateBubble(assistantBubble, 'Errore: il servizio non ha risposto. Riprova tra poco.', true);
        }
    } finally {
        setSending(false);
    }
});

function renderQuotaExceededBubble(wrapper) {
    const bubble = wrapper.querySelector('.bubble');
    bubble.classList.add('error');

    const content = wrapper.querySelector('.bubble-content');
    content.innerHTML = '';

    const text = document.createElement('div');
    text.textContent = 'Hai raggiunto il limite di richieste gratuite per questa finestra di 2 ore.';
    content.appendChild(text);

    const unlockButton = document.createElement('button');
    unlockButton.type = 'button';
    unlockButton.className = 'unlock-button';
    unlockButton.textContent = '💳 Sblocca illimitato (€1,00)';
    unlockButton.addEventListener('click', startCheckout);
    content.appendChild(unlockButton);

    chatLog.scrollTop = chatLog.scrollHeight;
}

async function sendTextMessage(message) {
    const response = await authFetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId, message })
    });
    if (!response.ok) {
        throw new Error('Request failed: ' + response.status);
    }
    return response.json();
}

async function sendImageMessage(message, files) {
    const formData = new FormData();
    files.forEach((file) => formData.append('images', file));
    if (message) {
        formData.append('message', message);
    }
    if (conversationId !== null) {
        formData.append('conversationId', conversationId);
    }
    const response = await authFetch('/api/chat/image', {
        method: 'POST',
        body: formData
    });
    if (!response.ok) {
        throw new Error('Request failed: ' + response.status);
    }
    return response.json();
}

function appendMessage(role, text, files) {
    const wrapper = document.createElement('div');
    wrapper.className = 'message ' + role;

    const bubble = document.createElement('div');
    bubble.className = 'bubble';

    if (files && files.length > 0) {
        const gallery = document.createElement('div');
        gallery.className = 'attached-images';
        files.forEach((file) => {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            img.className = 'attached-image';
            gallery.appendChild(img);
        });
        bubble.appendChild(gallery);
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

function renderHistoryMessage(role, content) {
    const wrapper = appendMessage(role, content);
    if (role === 'assistant') {
        wrapper.querySelector('.bubble-content').innerHTML = formatReply(content);
    }
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
