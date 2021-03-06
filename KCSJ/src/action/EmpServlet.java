package action;

import java.awt.image.PixelInterleavedSampleModel;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import Tool.picture;
import biz.EBiz.EmpBiz;
import biz.EBiz.EmpBizImpl;
import entity.Client;
import entity.History;
import entity.Menus;
import entity.Room;
import entity.Vip;
import javabean.ClientBean;
import javabean.VipBean;

@WebServlet("/Emp/*")
public class EmpServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public String t;

	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html;charset=utf-8");

		request.setCharacterEncoding("utf-8");// 设置request解码
		PrintWriter out = response.getWriter();

		HttpSession session = request.getSession();

		String url = request.getRequestURI();

		String path = url.substring(url.lastIndexOf("/"), url.lastIndexOf("."));

		EmpBiz biz = new EmpBizImpl();

		Date nowTime = new Date();
		SimpleDateFormat time = new SimpleDateFormat("yyyy/MM/dd/ HH:mm:ss");
		String t = time.format(nowTime);

		if ("/login".equals(path)) {

			String name = request.getParameter("name");

			String pwd = request.getParameter("pwd");

			// 获取用户输入验证码
			String code = request.getParameter("code");

			// 先从session中获取正确验证码

			String checkCode = (String) session.getAttribute("right_checkCode");

			String panduan = biz.queryEmp(name, pwd, code, checkCode);

			if (!panduan.equals("验证码错误")) {

				if ("正确".equals(panduan)) {
					// 登录成功后将员工名字和密码存入session
					session.setAttribute("ename", name);
					session.setAttribute("epwd", pwd);
					out.print("{\"result\":\"5\"}");

				} else if ("错误".equals(panduan)) {
					out.print("{\"result\":\"0\"}");
				} else {
					out.print("{\"result\":\"1\"}");
				}
			} else {
				out.print("{\"result\":\"2\"}");
			}
		} else if ("/password".equals(path)) {

			String card = request.getParameter("card");

			String password = request.getParameter("password");

			String str = biz.changePwd(card, password);

			if (str.equals("您输入的身份证有误，请重试")) {
				request.setAttribute("msg", "您输入的身份证有误，请重试");
				request.getRequestDispatcher("../Login/Emppassword.jsp").forward(request, response);

			} else if (str.equals("成功")) {
				response.sendRedirect("../Login/EmpLogin.jsp");

			} else {
				request.setAttribute("msg", "出错了");
				request.getRequestDispatcher("../Login/Emppassword.jsp").forward(request, response);
			}

		}
		// 预定入住
		else if ("/yuding".equals(path)) {
			String type = request.getParameter("idtype");
			String name = request.getParameter("name");
			String card = request.getParameter("idcard");
			long tel = Long.valueOf(request.getParameter("tel"));
			int rmno = Integer.valueOf(request.getParameter("rmno"));
			// 插入client不加时间
			boolean flag = biz.ClientYuDing(name, card, tel, rmno);
			if (flag == true) {
				// 跟新预定状态
				boolean flag2 = biz.updateRoomYuDing(rmno);
				// 将预定信息插入到历史纪录表，设置事件为预定
				boolean flag3 = biz.yudingHistory(name, card, tel, rmno, type, t, "预定房间");
				if (flag2 == true) {
					out.print("{\"result\":\"0\"}");
				}
			} else {
				out.print("{\"result\":\"1\"}");
			}

		} else if ("/tuiding".equals(path)) {
			int rmno = Integer.valueOf(request.getParameter("rmno"));
			// 退订删除顾客信息表，跟新房间
			String type = biz.queryRoomTypeByRmno(rmno);
			// System.out.println(rmno);
			if (type != null) {
				// System.out.println("成功");
				boolean flag = biz.tuiding(rmno);
				// 客户表中查找信息 逻辑错误 预定没有插入客户信息表
				// 在记录中找信息
				// 通过房间号查找房间类型
				Client client = biz.queryClientByRmno(rmno);
				// System.out.println(client);
				biz.addTuiDinghistory(client.getCname(), client.getCcard(), client.getCtel(), client.getRmno(), type, t,
						"退订");
				biz.deleteClient(rmno);
				if (flag == true) {
					out.print("{\"result\":\"0\"}");
				}
			} else {
				out.print("{\"result\":\"1\"}");
			}

		} else if ("/yudingruzhu".equals(path)) {
			int rmno = Integer.valueOf(request.getParameter("rmno"));
			// 判断是否是会员，
			// 通过房间号查找对应的信息
			Client c = biz.queryClientByRmno(rmno);
			String card = c.getCcard();
			// 在会员表中通过card查找
			String card2 = biz.queryVipByCard(card);
			// 如果是会员就将入住时间加入会员表,不是就加入客户表
			if (card2 == null) {
				// 普通客户预定入住，将客户信息表的时间加上
				boolean flag = biz.yudingruzhu(rmno);
				// 将房间的状态改成入住，无人预定
				boolean flag3 = biz.updateroomyudingruzhu(rmno);
				// 跟新房间的入住时间
				boolean flag2 = biz.roomyudingruzhu(rmno);
				// 查询出房间信息
				Client client = biz.queryClientByRmno(rmno);
				String type = biz.queryRoomTypeByRmno(rmno);
				// 预定住房记录
				biz.addRuZhuhistory(client.getCname(), client.getCcard(), client.getCtel(), client.getRmno(), type,
						"预定入住");
				if (flag == true) {
					out.print("{\"result\":\"0\"}");
				} else {
					out.print("{\"result\":\"1\"}");
				}
			} else if (card2 != null) {
				// 将时间加入会员
				boolean flag4 = biz.updateVipTime(rmno);
				// 将房间的状态改成入住，无人预定
				boolean flag3 = biz.updateroomyudingruzhu(rmno);
				// 查询出房间信息
				Client client = biz.queryClientByRmno(rmno);
				String type = biz.queryRoomTypeByRmno(rmno);
				// 预定住房记录
				biz.addRuZhuhistory(client.getCname(), client.getCcard(), client.getCtel(), client.getRmno(), type,
						"预定入住");
				boolean falg5 = biz.deleteClient(rmno);
				if (flag3 == true) {
					out.print("{\"result\":\"0\"}");
				} else {
					out.print("{\"result\":\"1\"}");
				}

			}

			// Vip预定
		} else if ("/VipYuDing".equals(path)) {
			int vno = Integer.valueOf(request.getParameter("vno"));
			String vcard = request.getParameter("vcard");
			int rmno = Integer.valueOf(request.getParameter("rmno"));
			Vip v = biz.QueryVipByVno(vno);
			// 判断会员号
			String card = v.getVcard();
			String name = v.getVname();
			long tel = v.getVtel();
			if (vcard.equals(card)) {
				// 调用预定方法更新房间状态
				boolean flag2 = biz.updateRoomYuDing(rmno);
				boolean flag = biz.ClientYuDing(name, card, tel, rmno);
				String type = biz.queryRoomTypeByRmno(rmno);
				// 会员预定 加入历史纪录
				biz.addRuZhuhistory(name, card, tel, rmno, type, "会员预定");
				// 会员预定加入会员表
				biz.updateVipMession(vno, rmno);
				if (flag2 == true) {
					out.print("{\"result\":\"0\"}");
				} else {
					out.print("{\"result\":\"1\"}");
				}
			} else {
				out.print("{\"result\":\"1\"}");
			}
		} else if ("/RuZhu".equals(path)) {

			String name = request.getParameter("name");

			String card = request.getParameter("card");

			String type = request.getParameter("type");

			long tel = Long.valueOf(request.getParameter("tel"));

			int rmno = Integer.valueOf(request.getParameter("rmno"));

			boolean flag = biz.ClientRuZhu(name, card, tel, rmno, type);

			if (flag) {
				// 房间的入住状态
				boolean flag1 = biz.updateRoomRuZhu(rmno);
				if (flag1 == true) {
					out.print("{\"result\":\"0\"}");
				}
			} else {
				out.print("{\"result\":\"1\"}");
			}

		} else if ("/updatePwd".equals(path)) {

			String oldpwd = request.getParameter("oldpwd");

			String newpwd = request.getParameter("newpwd");

			String ename = (String) session.getAttribute("ename");

			String str = biz.updateEmpPwd(oldpwd, newpwd, ename);

			if (str.equals("密码错误")) {

				out.print("{\"result\":\"0\"}");

			} else if (str.equals("成功修改")) {

				out.print("{\"result\":\"1\"}");
			} else {

				out.print("{\"result\":\"2\"}");
			}
		} else if ("/VipRuZhu".equals(path)) {
			int vno = Integer.valueOf(request.getParameter("vno"));
			String vcard = request.getParameter("vcard");

			String type = request.getParameter("type");

			int rmno = Integer.valueOf(request.getParameter("rmno"));

			String s = biz.VipRuZhu(vno, vcard, rmno, type);

			if (s.equals("信息有误")) {

				out.print("{\"result\":\"0\"}");
			} else if (s.equals("ok")) {
				out.print("{\"result\":\"1\"}");
			} else if (s.equals("erro")) {
				out.print("{\"result\":\"2\"}");
			} else {
				out.print("{\"result\":\"3\"}");
			}

		} else if ("roomNewMession".equals(path)) {
			int rmno = Integer.valueOf(request.getParameter("rmno"));
			Client list = biz.queryClientByRmno(rmno);
			Gson roomjson = new Gson();
			out.print(roomjson.toJson(list));
			// System.out.println("你好");
			if (list != null) {
				out.print(roomjson.toJson(list));
			} else {
				out.print(roomjson.toJson(null));
			}

		}

		else if ("/ClientLeave".equals(path)) {

			int rmno = Integer.valueOf(request.getParameter("sousuo"));

			// 查询普通客户的入住信息
			List<ClientBean> list = biz.queryClient_Leave(rmno);

			List<VipBean> listVip = biz.queryVip_Leave(rmno);

			Gson json = new Gson();

			if (list.size() == 1) {
				out.print(json.toJson(list));
			} else if (listVip.size() == 1) {
				out.print(json.toJson(listVip));

			} else {
				out.print(json.toJson(null));
			}

		} else if ("/VipLeave".equals(path)) {

			int vno = Integer.valueOf(request.getParameter("vno"));

			int rmno = Integer.valueOf(request.getParameter("rmno"));

			double mfee = Double.valueOf(request.getParameter("mfee"));

			double allfee = Double.valueOf(request.getParameter("allfee"));

			String card = request.getParameter("card");

			String name = request.getParameter("name");

			long tel = Long.valueOf(request.getParameter("tel"));

			String type = request.getParameter("type");

			String time1 = request.getParameter("time");

			String panduan = biz.VipLeave(vno, rmno, mfee, allfee, card, name, tel, type, time1);

			if (panduan.equals("success")) {

				out.print("{\"result\":\"0\"}");
			} else {
				out.print("{\"result\":\"1\"}");
			}

			// 普通客户退房后的操作
		} else if ("/ClientOfLeave".equals(path)) {
			int rmno = Integer.valueOf(request.getParameter("rmno"));

			double mfee = Double.valueOf(request.getParameter("mfee"));

			double allfee = Double.valueOf(request.getParameter("allfee"));

			String card = request.getParameter("card");

			String name = request.getParameter("name");

			long tel = Long.valueOf(request.getParameter("tel"));

			String type = request.getParameter("type");

			String time1 = request.getParameter("time");

			String panduan = biz.ClientLeave(rmno, mfee, allfee, card, name, tel, type, time1);

			if (panduan.equals("success")) {
				out.print("{\"result\":\"0\"}");
			} else {
				out.print("{\"result\":\"1\"}");
			}

		} else if ("/AllHistory".equals(path)) {

			List<History> list = biz.queryAllHistory();

			if (list != null) {
				request.setAttribute("historyList", list);
				request.getRequestDispatcher("../EmpJsp/History.jsp").forward(request, response);
			}

		} else if ("/ClientMenus".equals(path)) {

			List<Menus> list = biz.queryAllMenus();

			if (list != null) {
				request.setAttribute("list", list);
				request.getRequestDispatcher("../EmpJsp/ClientMenus.jsp").forward(request, response);
			}

		} else if ("/RuZhuMession".equals(path)) {
			List<Room> room = biz.QueryAllRoomByRmbuff();
			if (room != null) {
				request.setAttribute("Roomlist", room);
				request.getRequestDispatcher("../EmpJsp/ruzhuMession.jsp").forward(request, response);
			}

			// 顾客点菜(biz中区分vip和client)
		} else if ("/DianCai".equals(path)) {

			double clientprice = Double.valueOf(request.getParameter("clientprice"));

			double vipprice = Double.valueOf(request.getParameter("vipprice"));

			int rmno = Integer.valueOf(request.getParameter("rmno"));

			String panduan = biz.dianCan(clientprice, vipprice, rmno);

			if (panduan.equals("client")) {
				out.print("{\"result\":\"0\"}");
			} else if (panduan.equals("vip")) {
				out.print("{\"result\":\"1\"}");
			} else {
				out.print("{\"result\":\"2\"}");

			}
		} else if ("/ChangeMenusByType".equals(path)) {

			String type = request.getParameter("type");

			List<Menus> list = biz.queryMenusByType(type);

			Gson roomjson = new Gson();

			out.print(roomjson.toJson(list));

		} else if ("/ChangeMenusByName".equals(path)) {

			String mname = request.getParameter("mname");

			List<Menus> list = biz.queryMenusByName(mname);

			Gson roomjson = new Gson();

			out.print(roomjson.toJson(list));

		}

		
	}

}
