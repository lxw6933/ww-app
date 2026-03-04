import {el} from './dom.js';

/**
 * 过滤链管理器。
 * <p>
 * 支持链式添加“包含/排除 + 数据”条件、标签化预览与拖拽排序。
 * </p>
 */
export class FilterChainManager {

    /**
     * 构造方法。
     */
    constructor() {
        this.chainEl = el('filterChain');
        this.tagEl = el('filterTags');
        this.addButtonEl = el('btnAddFilter');
        this.draggingRow = null;
    }

    /**
     * 初始化默认规则与交互事件。
     */
    init() {
        this.bindRemoveAction();
        this.bindDragActions();
        this.bindTagActions();
        this.chainEl.addEventListener('input', () => this.renderTagPreview());
        this.chainEl.addEventListener('change', () => this.renderTagPreview());
        this.addRule('include', '');
        this.renderTagPreview();
    }

    /**
     * 绑定“新增条件”按钮。
     */
    bindAddAction() {
        this.addButtonEl.addEventListener('click', () => {
            this.addRule('include', '');
            this.emitChanged();
        });
    }

    /**
     * 给过滤输入框绑定回车事件。
     *
     * @param {Function} callback 回调函数
     */
    bindEnterAction(callback) {
        this.chainEl.addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                callback();
            }
        });
    }

    /**
     * 新增一条过滤规则行。
     *
     * @param {string} type 规则类型
     * @param {string} data 规则数据
     */
    addRule(type, data) {
        const row = document.createElement('div');
        row.className = 'filter-row';
        row.draggable = true;

        const dragButton = document.createElement('button');
        dragButton.className = 'btn-drag';
        dragButton.type = 'button';
        dragButton.textContent = '拖';
        dragButton.title = '拖拽排序';
        dragButton.tabIndex = -1;

        const typeSelect = document.createElement('select');
        typeSelect.className = 'filter-type';
        typeSelect.add(new Option('仅看包含词', 'include'));
        typeSelect.add(new Option('排除包含词', 'exclude'));
        typeSelect.value = type === 'exclude' ? 'exclude' : 'include';

        const dataInput = document.createElement('input');
        dataInput.className = 'filter-data';
        dataInput.type = 'text';
        dataInput.placeholder = '支持 && 与 ||，例如：异常&&超时 或 ERROR||WARN';
        dataInput.value = data || '';

        const removeButton = document.createElement('button');
        removeButton.className = 'btn-remove';
        removeButton.type = 'button';
        removeButton.textContent = '删除';
        removeButton.title = '删除条件';

        row.appendChild(dragButton);
        row.appendChild(typeSelect);
        row.appendChild(dataInput);
        row.appendChild(removeButton);
        this.chainEl.appendChild(row);
        this.renderTagPreview();
    }

    /**
     * 获取规范化后的规则列表。
     *
     * @returns {Array<{type:string,data:string}>} 规则集合
     */
    getRules() {
        const rules = [];
        this.chainEl.querySelectorAll('.filter-row').forEach(row => {
            const typeEl = row.querySelector('.filter-type');
            const dataEl = row.querySelector('.filter-data');
            const type = typeEl && typeEl.value ? typeEl.value.trim() : '';
            const data = dataEl && dataEl.value ? dataEl.value.trim() : '';
            if (!type || !data) {
                return;
            }
            rules.push({type: type, data: data});
        });
        return rules;
    }

    /**
     * 批量设置过滤规则。
     *
     * @param {Array<{type:string,data:string}>} rules 规则列表
     */
    setRules(rules) {
        this.chainEl.innerHTML = '';
        (rules || []).forEach(rule => {
            const type = rule && rule.type ? String(rule.type).trim() : 'include';
            const data = rule && rule.data ? String(rule.data).trim() : '';
            if (!data) {
                return;
            }
            this.addRule(type, data);
        });
        this.ensureAtLeastOneRule();
        this.renderTagPreview();
        this.emitChanged();
    }

    /**
     * 控制过滤链区域可编辑状态。
     *
     * @param {boolean} disabled 是否禁用
     */
    setDisabled(disabled) {
        this.addButtonEl.disabled = disabled;
        this.chainEl.querySelectorAll('select,input,button').forEach(node => {
            node.disabled = disabled;
        });
        this.chainEl.querySelectorAll('.filter-row').forEach(row => {
            row.draggable = !disabled;
        });
        if (this.tagEl) {
            this.tagEl.querySelectorAll('button').forEach(button => {
                button.disabled = disabled;
            });
        }
    }

    /**
     * 至少保留一条空规则，降低空白态操作门槛。
     */
    ensureAtLeastOneRule() {
        if (this.chainEl.querySelectorAll('.filter-row').length > 0) {
            return;
        }
        this.addRule('include', '');
    }

    /**
     * 绑定删除行为（行删除与标签删除）。
     */
    bindRemoveAction() {
        this.chainEl.addEventListener('click', event => {
            const target = event.target;
            if (!target || !target.classList.contains('btn-remove')) {
                return;
            }
            const row = target.closest('.filter-row');
            if (!row) {
                return;
            }
            this.chainEl.removeChild(row);
            this.ensureAtLeastOneRule();
            this.renderTagPreview();
            this.emitChanged();
        });
    }

    /**
     * 绑定拖拽排序行为。
     */
    bindDragActions() {
        this.chainEl.addEventListener('dragstart', event => {
            const row = event.target && event.target.closest ? event.target.closest('.filter-row') : null;
            if (!row || row.getAttribute('draggable') !== 'true') {
                return;
            }
            this.draggingRow = row;
            row.classList.add('dragging');
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = 'move';
                event.dataTransfer.setData('text/plain', 'move');
            }
        });

        this.chainEl.addEventListener('dragover', event => {
            if (!this.draggingRow) {
                return;
            }
            const targetRow = event.target && event.target.closest ? event.target.closest('.filter-row') : null;
            if (!targetRow || targetRow === this.draggingRow) {
                return;
            }
            event.preventDefault();
            const rect = targetRow.getBoundingClientRect();
            const placeBefore = event.clientY < rect.top + rect.height / 2;
            this.clearDragOverStyles();
            targetRow.classList.add('drag-over');
            if (placeBefore) {
                this.chainEl.insertBefore(this.draggingRow, targetRow);
            } else {
                this.chainEl.insertBefore(this.draggingRow, targetRow.nextSibling);
            }
        });

        this.chainEl.addEventListener('drop', event => {
            if (!this.draggingRow) {
                return;
            }
            event.preventDefault();
            this.clearDragOverStyles();
            this.renderTagPreview();
            this.emitChanged();
        });

        this.chainEl.addEventListener('dragend', () => {
            if (this.draggingRow) {
                this.draggingRow.classList.remove('dragging');
            }
            this.draggingRow = null;
            this.clearDragOverStyles();
        });
    }

    /**
     * 清理拖拽过程中的样式标识。
     */
    clearDragOverStyles() {
        this.chainEl.querySelectorAll('.filter-row.drag-over').forEach(row => row.classList.remove('drag-over'));
    }

    /**
     * 绑定标签操作（点击删除）。
     */
    bindTagActions() {
        if (!this.tagEl) {
            return;
        }
        this.tagEl.addEventListener('click', event => {
            const target = event.target;
            if (!target || !target.classList.contains('filter-tag-remove')) {
                return;
            }
            const index = parseInt(target.getAttribute('data-index') || '-1', 10);
            if (Number.isNaN(index) || index < 0) {
                return;
            }
            const rows = Array.from(this.chainEl.querySelectorAll('.filter-row'));
            const row = rows[index];
            if (!row) {
                return;
            }
            this.chainEl.removeChild(row);
            this.ensureAtLeastOneRule();
            this.renderTagPreview();
            this.emitChanged();
        });
    }

    /**
     * 渲染规则标签预览。
     */
    renderTagPreview() {
        if (!this.tagEl) {
            return;
        }
        this.tagEl.innerHTML = '';
        const rows = Array.from(this.chainEl.querySelectorAll('.filter-row'));
        const validRows = [];
        rows.forEach((row, index) => {
            const typeEl = row.querySelector('.filter-type');
            const dataEl = row.querySelector('.filter-data');
            const type = typeEl && typeEl.value ? String(typeEl.value).trim() : '';
            const data = dataEl && dataEl.value ? String(dataEl.value).trim() : '';
            if (!type || !data) {
                return;
            }
            validRows.push({index: index, type: type, data: data});
        });

        if (!validRows.length) {
            const emptyTag = document.createElement('span');
            emptyTag.className = 'filter-tag filter-tag-empty';
            emptyTag.textContent = '请输入关键词后自动生成条件标签';
            this.tagEl.appendChild(emptyTag);
            return;
        }

        validRows.forEach((rule, order) => {
            const tag = document.createElement('span');
            tag.className = rule.type === 'exclude' ? 'filter-tag filter-tag-exclude' : 'filter-tag';
            const typeText = rule.type === 'exclude' ? '排除' : '包含';
            tag.textContent = `${order + 1}. ${typeText}:${rule.data}`;

            const removeButton = document.createElement('button');
            removeButton.type = 'button';
            removeButton.className = 'filter-tag-remove';
            removeButton.setAttribute('data-index', String(rule.index));
            removeButton.title = '删除该条件';
            removeButton.textContent = '删';
            tag.appendChild(removeButton);
            this.tagEl.appendChild(tag);
        });
    }

    /**
     * 通知外部过滤链已变更。
     */
    emitChanged() {
        this.chainEl.dispatchEvent(new CustomEvent('chain:changed'));
    }
}
