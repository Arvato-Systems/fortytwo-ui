/*
 * Copyright (c) 2012 - 2018 Arvato Systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arvatosystems.t9t.tfi.viewmodel;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.Binder;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.image.AImage;
import org.zkoss.lang.Generics;
import org.zkoss.xel.fn.CommonFns;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Window;

import com.arvatosystems.t9t.authc.api.TenantDescription;
import com.arvatosystems.t9t.component.fields.IField;
import com.arvatosystems.t9t.tfi.general.ApplicationUtil;
import com.arvatosystems.t9t.tfi.general.Constants;
import com.arvatosystems.t9t.tfi.general.Constants.NaviConfig;
import com.arvatosystems.t9t.tfi.general.Maps;
import com.arvatosystems.t9t.tfi.model.bean.Navi;
import com.arvatosystems.t9t.tfi.services.IApplicationDAO;
import com.arvatosystems.t9t.tfi.viewmodel.navigation.NaviComparator;
import com.arvatosystems.t9t.tfi.viewmodel.navigation.NaviGroupingViewModel;
import com.arvatosystems.t9t.tfi.web.ApplicationSession;
import com.arvatosystems.t9t.tfi.web.CtrlKeyHandler;
import com.arvatosystems.t9t.tfi.web.ZulUtils;
import com.google.common.collect.ImmutableMap;

import de.jpaw.dp.Jdp;

/**
 * index View Model build the whole application.
 *
 * @author INCI02
 *
 */
public class ApplicationViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationViewModel.class);

    private String userInfo;
    private NaviGroupingViewModel naviGroupingViewModel = null;
    private Object selected;
    private Map<String, Panelchildren> naviContentMap = new HashMap<String, Panelchildren>();
    private boolean  previouslyCachingTypeWasCreateWithoutCaching=false;
    @Wire("#panel")
    private Panel panel;
    private String whenLastLoggedIn;
    private Long pwdExpiresInDays = null;
    private Integer numberOfIncorrentAttempts;
    private String selectedTenantId;
    private final ApplicationSession as = ApplicationSession.get();
    private final IApplicationDAO applicationDAO = Jdp.getRequired(IApplicationDAO.class);
    public final String APPLICATION_VERSION = ApplicationUtil.getVersionWithoutSnapshot();

    private String userName;
    private String userId;
    @SuppressWarnings("rawtypes")
    List<IField> filters;
    List<HtmlBasedComponent> htmlBasedFieldComponents;
    private boolean isDefaultOrder;

    public ApplicationViewModel() {

        if (as.getTenantId() == null) {
            Executions.sendRedirect(Constants.ZulFiles.LOGOUT);
        } else {
            selectedTenantId = as.getTenantId();
            Subject currentUser = SecurityUtils.getSubject();

            if (!currentUser.isAuthenticated()) {
                Executions.getCurrent().sendRedirect(Constants.ZulFiles.LOGIN);
            }

            setUserInfo(currentUser.getPrincipal().toString());

            userId = as.getUserId();
            userName = as.getJwtInfo().getName();
            if (userName == null)
                userName = "?";

            numberOfIncorrentAttempts = as.getNumberOfIncorrentAttempts();
            if (numberOfIncorrentAttempts == null)
                numberOfIncorrentAttempts = Integer.valueOf(0);
            Instant lastLoggedIn = as.getLastLoggedIn();
            if (lastLoggedIn != null) {
                whenLastLoggedIn = CommonFns.formatDate(lastLoggedIn.toDate(), ZulUtils.i18nLabel("com.datetime.format"));
            }
            Instant passwordExpires = as.getPasswordExpires();
            if (passwordExpires != null) {
                pwdExpiresInDays = passwordExpires.toDate().getTime() - System.currentTimeMillis();
                Double doublePwdExpires = Math.ceil(new Double(pwdExpiresInDays) / (1000 * 60 * 60 * 24));
                pwdExpiresInDays = doublePwdExpires.longValue();
            }

            LOGGER.info("New ApplicationViewModel created for user {}, now reading menu...", userId);
            as.readMenu();

            //Reset all screens in hash for each new reloading the menus
            /*FT-808*/  naviContentMap = new HashMap<String, Panelchildren>();
            //          paramMap = new HashMap<String, Object>();
            isDefaultOrder = Boolean.valueOf(ZulUtils.i18nLabel("isDefaultOrder"));
        }
    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        Selectors.wireComponents(view, this, false);
    }

    @Command
    public void onTenantClicked() {
        List<TenantDescription> allowedTenants= ApplicationSession.get().getAllowedTenants();

        if (allowedTenants.size() > 1) {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("isCancelClose", true);
                final Window win = (Window) Executions.createComponents(Constants.ZulFiles.LOGIN_TENANT_SELECTION, null, args);
                win.setClosable(true);
                win.setMode(Window.MODAL);
                win.doModal();
        }
    }

    /**
     * @return the pwdExpiresInDays
     */
    public Long getPwdExpiresInDays() {
        return pwdExpiresInDays;
    }
    /**
     * @param pwdExpiresInDays
     *            the pwdExpiresInDays to set
     */
    public void setPwdExpiresInDays(Long pwdExpiresInDays) {
        this.pwdExpiresInDays = pwdExpiresInDays;
    }
    /**
     * @return the whenLastLoggedIn
     */
    public final String getWhenLastLoggedIn() {
        return whenLastLoggedIn;
    }
    /**
     * @param whenLastLoggedIn
     *            the whenLastLoggedIn to set
     */
    public final void setWhenLastLoggedIn(String whenLastLoggedIn) {
        this.whenLastLoggedIn = whenLastLoggedIn;
    }
    /**
     *
     * @param hierarchy
     *            if not null than get only the given hierarchy
     */
    public final void setNavigation(Integer hierarchy) {

        if (hierarchy == null) {
            naviGroupingViewModel = new NaviGroupingViewModel(as.getAllNavigations(), new NaviComparator(), false);
        } else {
            naviGroupingViewModel = new NaviGroupingViewModel(applicationDAO.getNavigationByHierarchy(as, hierarchy), new NaviComparator(), false);
        }
    }

    /**
     * @return the userInfo
     */
    public final String getUserInfo() {
        return userInfo;
    }

    /**
     * @param userInfo
     *            the userInfo to set
     */
    public final void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }


    /**
     * @return the naviGroupingViewModel
     */
    public final NaviGroupingViewModel getNaviGroupingViewModel() {
        if (naviGroupingViewModel == null) {
            setNavigation(0);
        }
        return naviGroupingViewModel;
    }

    /**
     * @param naviGroupingViewModel
     *            the naviGroupingViewModel to set
     */
    public final void setNaviGroupingViewModel( NaviGroupingViewModel naviGroupingViewModel) {
        this.naviGroupingViewModel = naviGroupingViewModel;
    }

    /**
     * @return the selected
     */
    public final Object getSelected() {
        return selected;
    }

    /**
     * @param selected
     *            the selected to set
     */
    public final void setSelected(Object selected) {
        if (selected instanceof String) {
            setNaviGroup(String.valueOf(selected), true);
        } else {
            createComponents((Navi) selected);
            this.selected = selected;
            setNaviGroup(((Navi)selected).getCategory(), false);
        }
    }
    private static final Map<String, Object> NO_PARAMS = ImmutableMap.of();

    @GlobalCommand("setSelectedFromJump")
    //@NotifyChange({ "selected" })
    public final void setSelectedFromJump(@BindingParam("selected") Object selected,
            @BindingParam("backNaviLink") String backNaviLink

    ) {
        if (selected instanceof String) {
            setNaviGroup(String.valueOf(selected), true);
        } else {
            Navi navi = (Navi) selected;
            if (!naviContentMap.containsKey(navi.getNaviId())) {
                //createComponents(navi);
                createComponents(navi,  backNaviLink == null ? NO_PARAMS : Collections.singletonMap("paramBackNaviLink", backNaviLink), Constants.Application.CachingType.GET_CACHED);
                this.selected = selected;
                setNaviGroup((navi).getCategory(), false);
            } else {
                String targetZul = navi.getLink();
                ApplicationUtil.navJumpToScreen(targetZul,
                        backNaviLink == null ? NO_PARAMS : Collections.singletonMap("paramBackNaviLink", backNaviLink));
            }
        }
    }
    private void setNaviGroup(String category, boolean isClosePermitted) {
        Integer groupIndex = applicationDAO.getGroupIndexByCategory(category, naviGroupingViewModel);
        if (groupIndex != null) {
            if (!naviGroupingViewModel.isGroupOpened(groupIndex)) {
                naviGroupingViewModel.addOpenGroup(groupIndex.intValue());
            } else if (isClosePermitted && naviGroupingViewModel.isGroupOpened(groupIndex)) {
                naviGroupingViewModel.removeOpenGroup(groupIndex.intValue());
            }
        }
    }

    @Command
    @NotifyChange({"selected"})
    public final void setNaviSelection(@BindingParam("navi") Navi navi) {
        setSelected(navi);
    }
    /**
     *
     * @param navi
     *            Navigates to the selected menu item.
     */
    public final void createComponents(Navi navi) {
        createComponents(navi, null, Constants.Application.CachingType.GET_CACHED);
    }

    public final void createComponents(Navi navi, Map<String, Object> params, /*boolean isCached*/Constants.Application.CachingType cachingType) {

        panel.getChildren().clear();

//        if (previouslyCachingTypeWasCreateWithoutCaching) {
//            naviContentMap.clear();
//        }

        as.setRequestParams(params);

        Map<String, String> map = new HashMap<String, String>();
        map.put(NaviConfig.PERMISSION, navi.getPermission());
        map.put(NaviConfig.LINK, navi.getLink());

        String key = navi.getNaviId();

        if (previouslyCachingTypeWasCreateWithoutCaching) {
            panel.getChildren().clear();
            previouslyCachingTypeWasCreateWithoutCaching=false;
        }

        if (!naviContentMap.containsKey(key) ||
                (cachingType==Constants.Application.CachingType.CREATE_AND_CACH) ||
                (cachingType==Constants.Application.CachingType.CREATE_WITHOUT_CACHING)) {

            if (cachingType != Constants.Application.CachingType.CREATE_WITHOUT_CACHING) {
                naviContentMap.remove(key); // clear the content map
            }

            Component content = Executions.createComponents(navi.getLink(), null,  map);
            Panelchildren panelChildren = new Panelchildren();
            if ( cachingType==Constants.Application.CachingType.CREATE_WITHOUT_CACHING) {
                content.setId(key+"_"+Constants.Application.CachingType.CREATE_WITHOUT_CACHING);
                panelChildren.setId(key + "_" + Constants.Application.CachingType.CREATE_WITHOUT_CACHING);
            } else {
                content.setId(key);
                panelChildren.setId(key);
            }

            panelChildren.appendChild(content);
            panel.appendChild(panelChildren);

            if (cachingType == Constants.Application.CachingType.CREATE_WITHOUT_CACHING) {
                previouslyCachingTypeWasCreateWithoutCaching= true;
            }else{
                naviContentMap.put(key, panelChildren);
            }

        } else {
            panel.getChildren().clear();
            panel.appendChild(naviContentMap.get(key));
            panel.setFocus(true);
        }

        LOGGER.debug("----> {} {} {}","x", panel.getLastChild().getId(), panel.getLastChild().hashCode());
    }
    /*
     * @GlobalCommand("createLinkComponents") public final void
     * createLinkComponents(@BindingParam("naviLink") String naviLink,
     * @BindingParam("params") String params) {
     * with pre-populated/ pre-selected data Navi navi=
     * applicationDAO.getNavigationByLink(naviLink); //
     * this.contentDiv.getChildren().clear(); Map<String,String> map= new
     * HashMap<String,String>(); map.put("permission", navi.getPermission());
     * map.put("params", params);
     *
     * Integer groupIndex=applicationDAO.getGroupIndex(naviLink,
     * naviGroupingViewModel); if (groupIndex!=null) {
     * if (!naviGroupingViewModel.isGroupOpened(groupIndex)) {
     * naviGroupingViewModel.addOpenGroup(groupIndex.intValue()); } else
     * if (naviGroupingViewModel.isGroupOpened(groupIndex)) {
     * naviGroupingViewModel.removeOpenGroup(groupIndex.intValue());
     * naviGroupingViewModel.addOpenGroup(groupIndex.intValue()); } }
     *
     * this.selected = navi; Executions.createComponents(navi.getLink(),
     * contentCard, map); }
     */
    @GlobalCommand("createLinkComponents")
    @NotifyChange({"selected"})
    public final void createLinkComponents(
            @BindingParam("naviLink") String naviLink,
 @BindingParam("params") Map<String, Object> params,
            @BindingParam("CachingType") Constants.Application.CachingType CachingType) {

        Navi navi = applicationDAO.getNavigationByLink(as, naviLink);

        if (navi == null) {
            throw new IllegalArgumentException(String.format("The navigation link %s is not configured in the resource properties. " +
                    "Please check section:\n " +
                    "\tmenu.base= {\n"+
                    "\t\t...\n"+
                    "\t\tnavi_id, position, category,  name,  %s,  hierarchy,  permission,  closeGroup,  AuthenticationType availability (*=all), menuItemVisible,item-image\n"+
                    "\t\t...\n"+
                    "\t}",naviLink, naviLink));
        }
        // Map<String,String> map= new HashMap<String,String>();
        // map.put("permission", navi.getPermission());
        // map.put("params", params);
        if (CachingType == null) {
            throw new IllegalArgumentException("Wrong CachingType (null)");
        }

        // logging
        String paramsToDisplay;
        if (params instanceof Map) {
            Map<Object, Object> toStringMap = Generics.cast(params);
            paramsToDisplay = Maps.mapToString(toStringMap);
        } else {
            paramsToDisplay = params != null ? String.valueOf(params) : null;
        }
        LOGGER.debug("Jump->to:{}:{} with:params={}", naviLink, CachingType, paramsToDisplay);

        // menu focus
        if (navi.isMenuItemVisible()) {
            setNaviSelection(navi);
        }

        createComponents(navi, params, CachingType);

        /*
         * Integer groupIndex=applicationDAO.getGroupIndex(naviLink,
         * naviGroupingViewModel); if (groupIndex!=null) {
         * if (!naviGroupingViewModel.isGroupOpened(groupIndex)) {
         * naviGroupingViewModel.addOpenGroup(groupIndex.intValue()); } else
         * if (naviGroupingViewModel.isGroupOpened(groupIndex)) {
         * naviGroupingViewModel.removeOpenGroup(groupIndex.intValue());
         * naviGroupingViewModel.addOpenGroup(groupIndex.intValue()); } }
         *
         * this.selected = navi; Executions.createComponents(navi.getLink(),
         * contentCard, map);
         */
    }


    /**
     *
     * @param keyEvent
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    @Command
    public void ctrlKeyClick(  @org.zkoss.bind.annotation.BindingParam("item") KeyEvent keyEvent) {
        CtrlKeyHandler ctrlKeyHandler= new CtrlKeyHandler();
        //ctrlKeyHandler.ctrlKeyClick(keyEvent,  applicationDAO);
        ctrlKeyHandler.ctrlKeyClick(keyEvent,  this.htmlBasedFieldComponents);
    }
    @SuppressWarnings("rawtypes")
    @GlobalCommand
    @NotifyChange({ "filters"})
    public void registerFields(@BindingParam("filters") List<IField> filters) {
        this.filters = filters;
        registerFieldComponents(this.filters);
    }

    @SuppressWarnings("rawtypes")
    public void registerFieldComponents(List<IField> filters) {
        if (filters.isEmpty()) {
            this.htmlBasedFieldComponents = null;
        } else {
            htmlBasedFieldComponents = new ArrayList<HtmlBasedComponent>();
            for (IField filter:filters) {
                for (Object component:filter.getComponents()) {
                    htmlBasedFieldComponents.add((HtmlBasedComponent) component);
                }
            }
        }
    }

    /**
     * @param contentDiv
     *            the contentDiv to set
     */
    @Command
    public final void setPanel(
            @BindingParam("panel") Panel panel) {
        this.panel = panel;

        if (this.selected == null) {
            // initial set selected to the first item
            // setSelected(naviGroupingViewModel.getChild(0, 0));
        }
    }
    /**
     * @return the selectedTenantId
     */
    public final String getSelectedTenantId() {
        return selectedTenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getNumberOfIncorrentAttempts() {
        return numberOfIncorrentAttempts;
    }

    public void setNumberOfIncorrentAttempts(Integer numberOfIncorrentAttempts) {
        this.numberOfIncorrentAttempts = numberOfIncorrentAttempts;
    }

    public AImage getTenantLogo() {
        return as.getTenantLogo();
    }

    public String getTenantResource(String resource) {
        return as.getTenantResource(resource);
    }

    public boolean isDefaultOrder() {
        return isDefaultOrder;
    }

    public void setDefaultOrder(boolean isDefaultOrder) {
        this.isDefaultOrder = isDefaultOrder;
    }
}
