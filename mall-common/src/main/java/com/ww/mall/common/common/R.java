package com.ww.mall.common.common;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description:
 * @author: ww
 * @create: 2021-04-16 09:51
 */
public class R extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	public R() {
		put("code", 0);
		put("msg", "success");
	}

	public static R error() {
		return error(500, "系统繁忙，请稍后再试");
	}

	public static R error(String msg) {
		return error(500, msg);
	}

	public static R error(int code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		return r;
	}

	public static R ok(Map<String, Object> map) {
		R r = new R();
		r.putAll(map);
		return r;
	}

	public static R oks(Map<String, Object> map) {
		R r = new R();
		r.put("data", map);
		return r;
	}

	public static R oks(Object object) {
		R r = new R();
		r.put("data", object);
		return r;
	}

	public static R ok() {
		return new R();
	}

	/**
	 * 分页封装
	 * @param pageInfo 分页信息
	 * @return R
	 */
	public static R ok(PageInfo pageInfo) {
		R r = new R();
		r.put("total", pageInfo.getTotal());
		r.put("page", pageInfo.getPageNum());
		r.put("limit", pageInfo.getPageSize());
		r.put("pages", pageInfo.getPages());
		r.put("list", pageInfo.getList());
		return r;
	}

	@Override
	public final R put(String key, Object value) {
		super.put(key, value);
		return this;
	}

	public static Object oks(){
		Map<String,Object> obj=new HashMap<>();
		obj.put("code", 0);
		obj.put("msg", "成功");
		return obj;
	}

	public static Object ok(Object data){
		Map<String,Object> obj=new HashMap<>();
		obj.put("code",0);
		obj.put("msg", "成功");
		if(Objects.isNull(data)){
			obj.put("data", null);
		}else {
			obj.put("data", data);
		}

		return obj;
	}
	public static Object ok(Object data, Object sum){
		Map<String,Object> obj=new HashMap<>(16);
		obj.put("code",0);
		obj.put("msg", "成功");
		obj.put("data", data);
		obj.put("sum", sum);
		return obj;
	}
	public static Object okList(List list){
		Map<String, Object> data = new HashMap<>(16);
		data.put("list", list);
		if (list instanceof Page){
			Page page= (Page) list;
			data.put("total", page.getTotal());
			data.put("page", page.getPageNum());
			data.put("limit", page.getPageSize());
			data.put("pages", page.getPages());
		} else {
			data.put("total", list.size());
			data.put("page", 1);
			data.put("limit", list.size());
			data.put("pages", 1);
		}
		return ok(data);
	}

	public static R okLists(List list){
		Map<String,Object> data = new HashMap<>(16);
		data.put("data", list);
		if (list instanceof Page){
			Page page= (Page) list;
			data.put("total", page.getTotal());
			data.put("page", page.getPageNum());
			data.put("limit", page.getPageSize());
			data.put("pages", page.getPages());
		}else {
			data.put("total", list.size());
			data.put("page", 1);
			data.put("limit", list.size());
			data.put("pages", 1);
		}
		return oks(data);
	}

	public static Object okList(List list, String avatar){
		Map<String,Object> data = new HashMap<>(16);
		data.put("list", list);
		data.put("avatar", avatar);
		if (list instanceof Page){
			Page page= (Page) list;
			data.put("total", page.getTotal());
			data.put("page", page.getPageNum());
			data.put("limit", page.getPageSize());
			data.put("pages", page.getPages());
		}else {
			data.put("total", list.size());
			data.put("page", 1);
			data.put("limit", list.size());
			data.put("pages", 1);
		}
		return ok(data);
	}
	public static Object okPageInfo(PageInfo pageInfo){
		Map<String,Object> data = new HashMap<>(16);
		data.put("list", pageInfo.getList());
		data.put("total", pageInfo.getTotal());
		data.put("page", pageInfo.getPageNum());
		data.put("limit", pageInfo.getPageSize());
		data.put("pages", pageInfo.getPages());
		return ok(data);
	}
	public static Object okList(List list, List pagedList) {
		Map<String, Object> map = new HashMap<String, Object>(16);

		if (pagedList instanceof Page) {
			Page page = (Page) pagedList;
			map.put("total", page.getTotal());
			map.put("page", page.getPageNum());
			map.put("limit", page.getPageSize());
			map.put("pages", page.getPages());
		}
		else{
			map.put("total", pagedList.size());
			map.put("page", 1);
			map.put("limit", pagedList.size());
			map.put("pages", 1);
		}
		map.put("list", list);
		return ok(map);
	}

	public static Object fail(){
		Map<String,Object> obj = new HashMap<>(16);
		obj.put("code", -10);
		obj.put("msg", "系统繁忙，请稍后再试");
		return obj;
	}

	public static Object fail(Integer code,String msg){
		Map<String,Object> map = new HashMap<>(16);
		map.put("code", code);
		map.put("msg", msg);
		return map;
	}

	public static Object fail(Integer code,String msg1, String msg2){
		Map<String,Object> map = new HashMap<>(16);
		map.put("code", code);
		map.put("msg1", msg1);
		map.put("msg2", msg2);
		return map;
	}

	public static Object unlogin() {
		return fail(501, "登陆信息过期，请重新登陆");
	}

}
