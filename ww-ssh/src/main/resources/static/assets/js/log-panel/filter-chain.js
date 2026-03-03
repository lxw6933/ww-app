import {el} from './dom.js';

/**
 * 过滤链管理器。
 * <p>
 * 支持链式添加“包含/排除 + 数据”条件，负责规则编辑与序列化。
 * </p>
 */
export class FilterChainManager {

    /**
     * 构造方法。
     */
    constructor() {
        this.chainEl = el('filterChain');
        this.addButtonEl = el('btnAddFilter');
    }

    /**
     * 初始化默认规则与事件。
     */
    init() {
        this.chainEl.addEventListener('click', event => {
            const target = event.target;
            if (target && target.classList.contains('btn-remove')) {
                const row = target.closest('.filter-row');
                if (row) {
                    this.chainEl.removeChild(row);
                }
            }
        });
        this.addRule('include', '');
    }

    /**
     * 绑定“新增条件”按钮。
     */
    bindAddAction() {
        this.addButtonEl.addEventListener('click', () => this.addRule('include', ''));
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

        const typeSelect = document.createElement('select');
        typeSelect.className = 'filter-type';
        typeSelect.add(new Option('仅看包含词', 'include'));
        typeSelect.add(new Option('排除包含词', 'exclude'));
        typeSelect.value = type === 'exclude' ? 'exclude' : 'include';

        const dataInput = document.createElement('input');
        dataInput.className = 'filter-data';
        dataInput.type = 'text';
        dataInput.placeholder = '输入关键词，例如：异常、超时、下单成功';
        dataInput.value = data || '';

        const removeButton = document.createElement('button');
        removeButton.className = 'btn-remove';
        removeButton.type = 'button';
        removeButton.textContent = '删除';
        removeButton.title = '删除条件';

        row.appendChild(typeSelect);
        row.appendChild(dataInput);
        row.appendChild(removeButton);
        this.chainEl.appendChild(row);
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
            const type = (typeEl && typeEl.value) ? typeEl.value.trim() : '';
            const data = (dataEl && dataEl.value) ? dataEl.value.trim() : '';
            if (!type || !data) {
                return;
            }
            rules.push({type: type, data: data});
        });
        return rules;
    }

    /**
     * 控制过滤链区域可编辑状态。
     *
     * @param {boolean} disabled 是否禁用
     */
    setDisabled(disabled) {
        this.addButtonEl.disabled = disabled;
        this.chainEl.querySelectorAll('select,input,button').forEach(input => {
            input.disabled = disabled;
        });
    }
}
