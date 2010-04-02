<%--
/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2002-2008 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified 
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 *
 * 2009 October 28: Created jonathan@opennms.org
 * 
 * Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 */
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Review Report" />
  <jsp:param name="headTitle" value="Review Report" />
  <jsp:param name="breadcrumb" value="<a href='report/index.jsp'>Reports</a>" />
  <jsp:param name="breadcrumb" 
		value="<a href='report/database/index.htm'>Database</a>" />
  <jsp:param name="breadcrumb" value="Run"/>
</jsp:include>

<h3>Review Report</h3>

	  <p>You are about to run the following report:</p>
	  <table>
	  		<td>Report Name</td>
			<td><c:out value="${criteria.displayName}"/></td>
	  <%-- // date fields --%>
	  <c:forEach items="${criteria.dateParms}" var="dateParm" >
			<tr>
				<td><c:out value="${dateParm.displayName}"/></td>
				<td><c:out value="${dateParm.value}"/></td>
			</tr>
	  </c:forEach>
	  <%-- // category fields --%>
		<c:forEach items="${criteria.stringParms}" var="stringParm" >
			<tr>
				<td><c:out value="${stringParm.displayName}"/></td>
				<td><c:out value="${stringParm.value}"/></td>
            </tr>
		</c:forEach>
	  </table>
	  <br/>
	  
<form:form>  
    <input type="submit" id="proceed" name="_eventId_proceed" value="Proceed" />&#160;
	<input type="submit" name="_eventId_revise" value="Revise"/>&#160;
	<input type="submit" name="_eventId_cancel" value="Cancel"/>&#160;
</form:form>

<jsp:include page="/includes/footer.jsp" flush="false" />