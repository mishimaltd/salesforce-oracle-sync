# Salesforce Sync

This project is a POC of a scalable and reusable Change Data Capture (CDC) tool for Salesforce.

The goal is to provide an efficient and cost-effective solution to the problem of moving data out of Salesforce into the Cengage ecosystem.

## Capturing Changes

The POC leverages Salesforce Platform Events to capture and transmit changes to in-scope Salesforce objects. The platform event used to capture data changes is called `ChangeCaptureEvent__e` and has the following attributes:

| Label | API name | Data Type | Description |
|-------|----------|-----------|-------------|
| `Action` | `Action__c` | `Text(10)` | One of (Create/Update/Delete) |
| `Payload` | `Payload__c` | `Long Text Area(100000)` | JSON payload of changes to Object |
| `sObjectId` | `sObjectId__c` | `Text(20)` | Salesforce Id of updated Object | 
| `sObjectType` | `sObjectType__c` | `Text(50)` | Object type of updated Object |

## Enabling Objects for CDC

To start capturing changes on a Salesforce object, a Trigger must be created on that object. The Trigger must be set up using the below format:

```
trigger <TriggerName> on <Object to enable CDC for> (after insert, after update, after delete, after undelete) {

    if(Trigger.isInsert) {
    	ChangeEventHandler.handleCreate(Trigger.newMap);
    }

    if(Trigger.isUpdate) {
        ChangeEventHandler.handleUpdate(Trigger.newMap,Trigger.oldMap);
    }

    if(Trigger.isDelete) {
        ChangeEventHandler.handleDelete(Trigger.oldMap);
    }

    if(Trigger.isUndelete) {
        ChangeEventHandler.handleCreate(Trigger.newMap);
    }
}

``` 

The Trigger delegates to the class `ChangeEventHandler` that can capture changes for any Salesforce object. 

## Generating Payloads

The `ChangeEventHandler` Apex class can extract change details for any collection of Salesforce object types. 

`Create` - Captures all non-null fields on the object

`Update` - Captures all fields that have changed between the previous and current version of the object

`Delete` - Does not capture any fields, just records the change as a 'Delete' event

`Undelete` - Captures all non-null fields on the undeleted object and treats the action as a 'Create'
   
The class serializes all field updates as key-value pairs in a JSON string and stores it in the `Payload__c` field of the Platform Event.   

## Deltas vs Full Payload

The `ChangeEventHandler` Apex class can be configured either to only capture changed fields or to capture the entire object (including null values). The advantage of sending deltas only is that message payloads are smaller. The advantage of sending full payloads is that if an error occurs during downstream processing an event, it can be resolved by simply touching the affected object (since the full representation of that object will be captured and re-sent). It does this by examining the schema for an Object and performing the following actions depending on the action that occurred to the Object:

## Capturing CDC events

The POC includes a CometD client that will subscribe to generated platform events. Platform events are received in the following JSON format:

```
{
	"schema": "QMFxKP-2r8_RkkO4FenaPA",
	"payload": {
		"CreatedById": "00522000001ACACAA4",
		"Payload__c": "{\"marketing_name__c\":\"Title\",\"marketing_description__c\":\"Description\",\"good_type__c\":\"Print\",\"pdlm__phase_unique__c\":\"a1B22000000G1KW:Active\",\"pdlm__lifecycle_phase__c\":\"Active\",\"systemmodstamp\":\"2020-06-16T14:37:11.000Z\",\"lastmodifieddate\":\"2020-06-16T14:37:11.000Z\"}",
		"CreatedDate": "2020-06-16T14:37:12.129Z",
		"sObjectType__c": "PDLM__Item_Revision__c",
		"Action__c": "Update",
		"sObjectId__c": "a1722000000xt66AAA"
	},
	"event": {
		"replayId": 568504
	}
}
``` 

For the purposes of the POC, the CometD client receives the above payload and converts to an internal ChangeEvent representation that looks like:

```
 {
 	"replayId": 568505,
 	"action": "Update",
 	"objectId": "a1722000000xt66AAA",
 	"objectType": "PDLM__Item_Revision__c",
 	"createdTime": "Jun 16, 2020, 10:40:38 AM",
 	"payload": "{\"marketing_name__c\":\"Title\",\"marketing_description__c\":\"Description\",\"good_type__c\":\"Print\",\"pdlm__phase_unique__c\":\"a1B22000000G1KW:Active\",\"pdlm__lifecycle_phase__c\":\"Active\",\"systemmodstamp\":\"2020-06-16T14:40:38.000Z\",\"lastmodifieddate\":\"2020-06-16T14:40:38.000Z\"}"
 }
```

The POC then pushes the ChangeEvent to a queue. The intent is that a managed component (e.g. Informatica) can be configured to listen on the queue and then use a managed ETL mapping process to process updates into a target schema (e.g. the ODS). The value in the `objectType` attribute can be used to map to a target database table and the value in the `objectId` attribute can be used as a target primary key on the table. 
The POC includes a simple listener which receives messages on the queue and drops them into a database table.

If a Salesforce connector is available (either using on-prem Informatica or Informatica Cloud) then it could be configured to subscribe directly to the CometD channel.

### Notes

The current limit for streaming API events on Salesforce production is 200000. 

Platform events are assigned an incrementing `replayId` value by Salesforce. The POC keeps track of the last received `replayId` and passes this in as an argument when opening the channel. This instructs Salesforce to only send Platform Events generated since that value.
