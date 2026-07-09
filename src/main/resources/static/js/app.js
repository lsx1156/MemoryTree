const API_BASE = 'http://localhost:8080/api';

let currentTab = 'inference';
let isInferencing = false;

document.addEventListener('DOMContentLoaded', () => {
    updateSystemStatus();
    loadBranches();
    loadPersistentMemory();
    updateMemoryStats();
    
    setInterval(updateSystemStatus, 5000);
    setInterval(updateMemoryStats, 3000);
});

function switchTab(tab) {
    currentTab = tab;
    
    document.querySelectorAll('[id^="content-"]').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('[id^="tab-"]').forEach(el => {
        el.classList.remove('bg-primary-600', 'text-white');
        el.classList.add('text-gray-300', 'hover:bg-gray-700');
    });
    
    document.getElementById(`content-${tab}`).classList.remove('hidden');
    document.getElementById(`tab-${tab}`).classList.remove('text-gray-300', 'hover:bg-gray-700');
    document.getElementById(`tab-${tab}`).classList.add('bg-primary-600', 'text-white');
    
    if (tab === 'branches') loadBranches();
    if (tab === 'memory') {
        loadWorkingMemory();
        loadPersistentMemory();
    }
}

function updateSystemStatus() {
    fetch(`${API_BASE}/inference/status`)
        .then(res => res.text())
        .then(text => {
            const statusEl = document.getElementById('system-status');
            if (text.includes('RUNNING')) {
                statusEl.innerHTML = '<i class="fa fa-circle text-green-500 mr-1"></i>运行中';
                statusEl.className = 'px-3 py-1 bg-green-900/50 text-green-400 rounded-full text-sm';
            } else {
                statusEl.innerHTML = '<i class="fa fa-circle text-yellow-500 mr-1"></i>初始化中';
                statusEl.className = 'px-3 py-1 bg-yellow-900/50 text-yellow-400 rounded-full text-sm';
            }
        })
        .catch(() => {
            document.getElementById('system-status').innerHTML = '<i class="fa fa-circle text-red-500 mr-1"></i>未连接';
        });
}

function updateMemoryStats() {
    fetch(`${API_BASE}/memory/working`)
        .then(res => res.json())
        .then(data => {
            let size = 0;
            data.forEach(item => {
                size += (item.content?.length || 0) * 2 + 100;
            });
            document.getElementById('working-memory-size').textContent = Math.round(size / 1024);
        });
    
    fetch(`${API_BASE}/memory/count`)
        .then(res => res.text())
        .then(count => {
            document.getElementById('persistent-memory-count').textContent = count;
        });
    
    fetch(`${API_BASE}/branch/list`)
        .then(res => res.json())
        .then(data => {
            const active = data.filter(b => b.active).length;
            document.getElementById('active-branch-count').textContent = active;
        });
}

function executeInference() {
    if (isInferencing) return;
    
    const prompt = document.getElementById('prompt-input').value.trim();
    if (!prompt) {
        alert('请输入推理问题');
        return;
    }
    
    isInferencing = true;
    const btn = document.getElementById('infer-btn');
    btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 推理中...';
    btn.disabled = true;
    
    resetStateMachine();
    setState('idle', 'active');
    
    const premises = document.getElementById('premises-input').value
        .split('\n')
        .map(p => p.trim())
        .filter(p => p);
    
    const request = {
        prompt: prompt,
        premises: premises,
        config: {
            temperature: parseFloat(document.getElementById('temperature-input').value),
            topP: 0.9,
            maxLength: 500,
            seed: Date.now(),
            useKVCache: true
        },
        maxIntrospectionRounds: parseInt(document.getElementById('introspection-rounds').value),
        enableMemoryRetrieval: true
    };
    
    setState('draft', 'active');
    
    fetch(`${API_BASE}/inference/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
    .then(res => res.json())
    .then(response => {
        setState('draft', 'completed');
        setState('validate', 'active');
        setState('validate', 'completed');
        setState('output', 'active');
        
        displayResult(response);
        
        setTimeout(() => {
            isInferencing = false;
            btn.innerHTML = '<i class="fa fa-play"></i> 执行逻辑推理';
            btn.disabled = false;
            setState('idle', 'active');
        }, 500);
    })
    .catch(err => {
        console.error(err);
        document.getElementById('result-content').textContent = '推理失败: ' + err.message;
        isInferencing = false;
        btn.innerHTML = '<i class="fa fa-play"></i> 执行逻辑推理';
        btn.disabled = false;
        setState('idle', 'active');
    });
}

function resetStateMachine() {
    ['idle', 'draft', 'validate', 'rewrite', 'output'].forEach(state => {
        const el = document.getElementById(`state-${state}`);
        el.className = 'flex items-center gap-2 text-gray-400';
        el.querySelector('span').className = 'w-3 h-3 rounded-full bg-gray-600';
    });
}

function setState(state, status) {
    const el = document.getElementById(`state-${state}`);
    if (!el) return;
    
    if (status === 'active') {
        el.className = 'flex items-center gap-2 text-primary-400';
        el.querySelector('span').className = 'w-3 h-3 rounded-full bg-primary-500 animate-pulse';
    } else if (status === 'completed') {
        el.className = 'flex items-center gap-2 text-logic-400';
        el.querySelector('span').className = 'w-3 h-3 rounded-full bg-logic-500';
    }
}

function displayResult(response) {
    document.getElementById('result-content').textContent = response.finalResult || '';
    
    const treeEl = document.getElementById('derivation-tree');
    treeEl.innerHTML = '';
    (response.derivationTree || []).forEach((step, i) => {
        const li = document.createElement('li');
        li.className = 'flex items-start gap-2';
        li.innerHTML = `
            <span class="w-6 h-6 bg-primary-600 rounded flex items-center justify-center text-xs flex-shrink-0">${i + 1}</span>
            <span>${step}</span>
        `;
        treeEl.appendChild(li);
    });
    
    const arbEl = document.getElementById('arbitration-result');
    const arb = response.arbitrationResult;
    if (arb) {
        let statusClass = 'text-gray-400';
        let statusText = '未知';
        if (arb.result === 'COMPLIANT') {
            statusClass = 'text-logic-400';
            statusText = '合规通过';
        } else if (arb.result === 'NON_COMPLIANT') {
            statusClass = 'text-red-400';
            statusText = '不合规';
        } else if (arb.result === 'LOW_CONFIDENCE') {
            statusClass = 'text-yellow-400';
            statusText = '低置信度';
        } else if (arb.result === 'FAIL_SAFE_TRIGGERED') {
            statusClass = 'text-red-500';
            statusText = 'FAIL-SAFE触发';
        }
        
        arbEl.innerHTML = `
            <div class="${statusClass} font-semibold">结果: ${statusText}</div>
            <div class="text-gray-400">合规分数: ${arb.complianceScore?.toFixed(2) || 'N/A'}</div>
            <div class="text-gray-400">说明: ${arb.explanation || '无'}</div>
            ${arb.violatingClauseName ? `<div class="text-red-400">违反规则: ${arb.violatingClauseName}</div>` : ''}
        `;
    }
    
    document.getElementById('stat-rounds').textContent = response.introspectionRounds || 0;
    document.getElementById('stat-compliance').textContent = (arb?.complianceScore || 0).toFixed(2);
    document.getElementById('stat-duration').textContent = response.totalDurationMs + 'ms';
    document.getElementById('stat-confidence').textContent = response.confidenceLow ? '低' : '正常';
}

function loadWorkingMemory() {
    fetch(`${API_BASE}/memory/working`)
        .then(res => res.json())
        .then(data => {
            const listEl = document.getElementById('working-memory-list');
            if (data.length === 0) {
                listEl.innerHTML = '<p class="text-gray-500 text-center py-8">工作记忆为空</p>';
                return;
            }
            
            listEl.innerHTML = data.map(item => `
                <div class="bg-gray-700/50 rounded p-3 mb-2">
                    <p class="text-gray-100">${item.content}</p>
                    <div class="flex items-center justify-between mt-2">
                        <span class="text-xs text-gray-500">${item.tags?.join(', ') || ''}</span>
                        <button onclick="removeFromWorkingMemory('${item.id}')" class="text-xs text-red-400 hover:text-red-300">
                            移除
                        </button>
                    </div>
                </div>
            `).join('');
        });
}

function loadPersistentMemory() {
    fetch(`${API_BASE}/memory/all`)
        .then(res => res.json())
        .then(data => {
            const listEl = document.getElementById('persistent-memory-list');
            if (data.length === 0) {
                listEl.innerHTML = '<p class="text-gray-500 text-center py-8">暂无持久记忆</p>';
                return;
            }
            
            listEl.innerHTML = data.map(item => `
                <div class="bg-gray-700/50 rounded p-3 mb-2">
                    <p class="text-gray-100">${item.content}</p>
                    <div class="flex items-center justify-between mt-2">
                        <span class="text-xs text-gray-500">${item.tags?.join(', ') || ''}</span>
                        <div class="flex gap-2">
                            <button onclick="injectToWorkingMemory('${item.id}')" class="text-xs text-primary-400 hover:text-primary-300">
                                注入
                            </button>
                            <button onclick="deleteMemory('${item.id}')" class="text-xs text-red-400 hover:text-red-300">
                                删除
                            </button>
                        </div>
                    </div>
                </div>
            `).join('');
        });
}

function storeMemory() {
    const content = document.getElementById('new-memory-content').value.trim();
    if (!content) {
        alert('请输入记忆内容');
        return;
    }
    
    const tags = document.getElementById('new-memory-tags').value
        .split(',')
        .map(t => t.trim())
        .filter(t => t);
    
    fetch(`${API_BASE}/memory/store`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content, tags })
    })
    .then(() => {
        document.getElementById('new-memory-content').value = '';
        document.getElementById('new-memory-tags').value = '';
        loadPersistentMemory();
        updateMemoryStats();
    });
}

function searchMemory() {
    const keyword = document.getElementById('search-keyword').value.trim();
    if (!keyword) {
        loadPersistentMemory();
        return;
    }
    
    fetch(`${API_BASE}/memory/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ keyword, limit: 20 })
    })
    .then(res => res.json())
    .then(data => {
        const listEl = document.getElementById('persistent-memory-list');
        if (data.length === 0) {
            listEl.innerHTML = '<p class="text-gray-500 text-center py-8">未找到匹配的记忆</p>';
            return;
        }
        
        listEl.innerHTML = data.map(item => `
            <div class="bg-gray-700/50 rounded p-3 mb-2">
                <p class="text-gray-100">${item.content}</p>
                <div class="flex items-center justify-between mt-2">
                    <span class="text-xs text-gray-500">${item.tags?.join(', ') || ''}</span>
                    <div class="flex gap-2">
                        <button onclick="injectToWorkingMemory('${item.id}')" class="text-xs text-primary-400 hover:text-primary-300">
                            注入
                        </button>
                        <button onclick="deleteMemory('${item.id}')" class="text-xs text-red-400 hover:text-red-300">
                            删除
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    });
}

function injectToWorkingMemory(id) {
    fetch(`${API_BASE}/memory/inject/${id}`, { method: 'POST' })
        .then(() => {
            loadWorkingMemory();
            updateMemoryStats();
        });
}

function removeFromWorkingMemory(id) {
    fetch(`${API_BASE}/memory/working/${id}`, { method: 'DELETE' })
        .then(() => {
            loadWorkingMemory();
            updateMemoryStats();
        });
}

function deleteMemory(id) {
    if (!confirm('确定要删除这条记忆吗？')) return;
    fetch(`${API_BASE}/memory/delete/${id}`, { method: 'DELETE' })
        .then(() => {
            loadPersistentMemory();
            updateMemoryStats();
        });
}

function resetWorkingMemory() {
    if (!confirm('确定要重置工作记忆吗？这将清空所有意识态记忆。')) return;
    fetch(`${API_BASE}/inference/reset`, { method: 'POST' })
        .then(() => {
            loadWorkingMemory();
            updateMemoryStats();
        });
}

function loadBranches() {
    fetch(`${API_BASE}/branch/list`)
        .then(res => res.json())
        .then(data => {
            const container = document.querySelector('#branches-list .grid');
            container.innerHTML = data.map(branch => `
                <div class="bg-gray-800 rounded-lg p-4">
                    <div class="flex items-center justify-between mb-2">
                        <h4 class="font-semibold text-gray-100">${branch.name}</h4>
                        <span class="px-2 py-1 rounded text-xs ${branch.active ? 'bg-logic-900/50 text-logic-400' : 'bg-gray-700 text-gray-500'}">
                            ${branch.active ? '活跃' : '停用'}
                        </span>
                    </div>
                    <p class="text-sm text-gray-400 mb-3">类型: ${branch.type}</p>
                    <button onclick="${branch.active ? `deactivateBranch('${branch.type}')` : `activateBranch('${branch.type}')`}" 
                        class="w-full py-2 rounded text-sm ${branch.active ? 'bg-red-600 hover:bg-red-700' : 'bg-logic-600 hover:bg-logic-700'}">
                        ${branch.active ? '停用树枝' : '激活树枝'}
                    </button>
                </div>
            `).join('');
        });
}

function activateBranch(type) {
    fetch(`${API_BASE}/branch/activate/${type}`, { method: 'POST' })
        .then(() => loadBranches());
}

function deactivateBranch(type) {
    fetch(`${API_BASE}/branch/deactivate/${type}`, { method: 'POST' })
        .then(() => loadBranches());
}