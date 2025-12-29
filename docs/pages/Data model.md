### A src video is queued
-
- asset
  logseq.order-list-type:: number
	- uuid
	  logseq.order-list-type:: number
	- type: video, image, audio
	  logseq.order-list-type:: number
	- meta-data
	  logseq.order-list-type:: number
	- file-hash
	  logseq.order-list-type:: number
	- path
	  logseq.order-list-type:: number
- pipeline (This has to do with the src file being queued to process various aspects, proxy, audio extraction etc.
  logseq.order-list-type:: number
	- asset being processed
	  logseq.order-list-type:: number
	- processes (the sub steps passed along with the src file to be run)
	  logseq.order-list-type:: number
- process
  logseq.order-list-type:: number
	- status: open | processing | completed
	  logseq.order-list-type:: number
	- deps: other processes this depends on
	  logseq.order-list-type:: number
	- artifacts: list of assets
	  logseq.order-list-type:: number
	- type: the type of process, proxy, audio extraction, transcribe, etc
	  logseq.order-list-type:: number