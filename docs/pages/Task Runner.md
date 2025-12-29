### Steps
	- DONE Pre process the rules and update with UUIDs
	  logseq.order-list-type:: number
	- Amend with the source file
	  logseq.order-list-type:: number
	- Write all the rules to the db
	  logseq.order-list-type:: number
	- Query for open jobs and process
	  logseq.order-list-type:: number
		- Either no parents deps are completed
		  logseq.order-list-type:: number
	- Process the returned batch
	  logseq.order-list-type:: number
-
- ### Questions
	- Do we need some type of spec for a rule? Inputs outputs?
	- Retries? Can we have backoff based on a failed state and query the history via xtdb and use that for increasing time?
	-