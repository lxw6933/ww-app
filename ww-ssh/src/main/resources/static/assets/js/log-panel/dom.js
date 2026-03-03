/**
 * 按 id 获取 DOM 元素。
 *
 * @param {string} id 元素 ID
 * @returns {HTMLElement}
 */
export function el(id) {
    return document.getElementById(id);
}

/**
 * 获取输入值，自动兜底为空字符串。
 *
 * @param {string} id 元素 ID
 * @returns {string}
 */
export function value(id) {
    return (el(id).value || '');
}

/**
 * 获取复选框选中状态。
 *
 * @param {string} id 元素 ID
 * @returns {boolean}
 */
export function checked(id) {
    return !!el(id).checked;
}
