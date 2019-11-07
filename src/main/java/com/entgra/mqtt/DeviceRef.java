/*
 *  Copyright (c) 2019, Entgra (pvt) Ltd. (http://entgra.io)
 *
 *  All Rights Reserved.
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited.
 *  Proprietary and confidential.
 */

package com.entgra.mqtt;

import java.util.List;

public class DeviceRef {
    private String id;
    private String type;
    private int placementX;
    private int placementY;
    private int placementId;
    private int styleId;
    private String styleName;
    private List<Integer> operationIds;

    public DeviceRef() {
    }

    public int getPlacementX() {
        return placementX;
    }

    public void setPlacementX(int placementX) {
        this.placementX = placementX;
    }

    public int getPlacementY() {
        return placementY;
    }

    public void setPlacementY(int placementY) {
        this.placementY = placementY;
    }

    public int getPlacementId() {
        return placementId;
    }

    public void setPlacementId(int placementId) {
        this.placementId = placementId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStyleId() {
        return styleId;
    }

    public void setStyleId(int styleId) {
        this.styleId = styleId;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public List<Integer> getOperationIds() {
        return operationIds;
    }

    public void setOperationIds(List<Integer> operationIds) {
        this.operationIds = operationIds;
    }

}