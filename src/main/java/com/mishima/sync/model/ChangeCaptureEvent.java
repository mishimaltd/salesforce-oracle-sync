package com.mishima.sync.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class ChangeCaptureEvent {

  @SerializedName("userName")
  private String userName;

  @SerializedName("action")
  private String action;

  @SerializedName("objectId")
  private String objectId;

  @SerializedName("objectType")
  private String objectType;

  @SerializedName("objectName")
  private String objectName;

  @SerializedName("createdDate")
  private Date createdTime;

  @SerializedName("oldFieldValues")
  private Map<String,Object> oldFieldValues;

  @SerializedName("newFieldValues")
  private Map<String,Object> newFieldValues;

  @SerializedName("fullPayload")
  private Map<String,Object> fullPayload;

}
