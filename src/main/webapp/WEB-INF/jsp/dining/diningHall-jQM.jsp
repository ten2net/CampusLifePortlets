<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

--%>

<jsp:directive.include file="/WEB-INF/jsp/include.jsp"/>
<c:set var="n"><portlet:namespace/></c:set>

<div id="${n}container" class="portlet">

    <div data-role="header" class="titlebar portlet-titlebar">
        <a class="menu-back-link" href="<portlet:renderURL/>" data-role="button" data-icon="back" data-inline="true">Back</a>
        <h2 class="title">${ diningHall.name }</h2>
    </div>

    <div data-role="content" class="portlet-content">
    
        <ul data-role="listview">
            <li data-role="list-divider">
                <div class="ui-grid-a">
                    <div class="ui-block-a"><h3><spring:message code="meals"/></h3></div>
                    <div class="ui-block-b">
                        <a href="${ locationUrl }" data-role="button" 
                                data-iconpos="notext" data-icon="map" data-theme="b">
                            <spring:message code="map"/>
                        </a>
                    </div>
                </div>
            </li>
            <c:forEach items="${ diningHall.meal }" var="meal" varStatus="status">
                <portlet:renderURL var="mealUrl">
                    <portlet:param name="action" value="meal"/>
                    <portlet:param name="diningHall" value="${ diningHall.key }"/>
                    <portlet:param name="mealName" value="${ meal.name }"/>
                </portlet:renderURL>
                <li><a href="${ mealUrl }">${ meal.name }</a></li>
            </c:forEach>
        </ul>
    
    </div>
</div>
