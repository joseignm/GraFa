This folder contains data for the paper "GraFa: Scalable Faceted Browsing for RDF Graphs using Selective Caching", under review at ISWC.

queryExperiments.csv describes the performance experiments run by emulating user sessions:
 - URL: indicates the URL of the query
 - time: response time in milliseconds
 - results: number of entities in the full result set
 - depth: how many facet selections have been performed
 - properties: how long the selection of a property value takes
 - size: the result size in bytes
 
 userStudy.csv describes the task-based user evaluation
 - ID: user id
 - Question: question/task ID (see userQuestions.csv in the same folder)
 - System: G: Grafa, W: Wikidata Query Service
 - Correct: Y: Yes, N: No, B: Blank (no response given)
 - Time: Time taken (ms) from question being displayed to response being given or question being skipped
 
 userStudyRaw.csv describes the responses given by users in the order received
 - User: user id
 - Key: [question id][A: Wikidata QueryService, B: Grafa][S: Skip, F:Answer submitted]
 - Response: value of answer submitted (URL of results page)
 - CurrentTime: UNIX time of response/skip
 
 userQuestions.csv:
 - ID: question ID
 - Text: question text
