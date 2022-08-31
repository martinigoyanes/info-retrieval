# Assignment 3

### Task 3.1

**What happens to the two documents that you selected?**
They appear higher on the top 10 relevant results.

**What are the characteristics of the other documents in the new top ten list - what are they about? Are there any new ones that were not among the top ten before?**
They are similiar to the documents selected as relevant. Yes, there are new ones that were not there before, this is because we have moved the centroid of our search, hence, the the top 10 closest documents to our query are different since we are in a different point inside the document's dimensional space.


**Try different values for the weights α and β: How is the relevance feedback process affected by α and β?**
The higher the alpha the more relevance we give to the current documents, i.e, the less the results change after giving feedback. The higher the beta, the more importance we give to the feedback given by the user, and the more the results change in the direction of the feedback given

**Why is the search after feedback slower? Why is the number of returned documents larger?**
After caclulating the modified query with the ROCCHIO algorithm we have a query that contains all the terms in the relevant documents weighted by beta/num_relevant_docs. So we might have done, initially, a query with 3 terms. We select 3 documents as relevant, and now our modified query, q_m, is 300 terms long, now we have to get the postings list of 300 terms which takes more time.

### Task 3.2

**1. Compute the normalized discounted cumulative gain (nDCG) at 50 for the query graduate program mathematics.**
DCG Ideal at 50 for query: graduate program mathematics is 20.85470647381484. And DCG Before Feedback at 50 for query: graduate program mathematics is 3.378714889109486. Therefore the nDCG at 50 is 0.16201210471851038.
**2. Mark "Mathematics.f" (the most relevant document according to everyone) in the search interface, and re-search (thereby applying relevance feedback).**
**3. Now compute the nDCG at 50 for the new results list. Do not include the "Mathematics.f" document in the computation, even if it is among the top 50. (QUESTION: Why do we want to omit that document?).**
DCG After Feedback at 50 for query: graduate program mathematics is 3.5601804686850134. Therefore the normalized DCG is 0.17071352565691464. We want to remove the Mathematics.f document because that is the "training sample" we gave our system to improve the result. Taking it into account would be like pumping up the metric we are using (nDCG).
**4. Compare your result in 1 and 3 above. What do you see?**
Now we get more relevant documents in the top 50. Feedback improved our nDCG by 1%.
