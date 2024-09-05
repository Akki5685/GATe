
**_GATe Tool, enhance user experience by simplifying the process, bypassing all the manual steps, and directly accessing the workflow to trigger jobs._**


In Continuous Integration process while doing through the workflow, we used to go through the following steps 
Open browser,
Navigate to GitHub, 
Get access through SSO, 
Open the Repo, 
Go to Workflow Folder, 
Select the workflow file, 
Make changes / commit /push the code / trigger the workflow dispatch to start the job, 
Check the status of workflow whether job starts/in progress/ pass/ fail/ completed,
Post execution we need the status of our test scripts i.e., html report. 

In case we have runner setup in remote desktop it will be difficult for us to fetch the report from there. 


![image](https://github.com/user-attachments/assets/baa28b10-6dd6-4e44-9589-74a135c2b559)














Owner  is the GitHub repo admin. Here gitActions is the repo and admin is the Akki5685 
Repository is the name of the repo i.e., gitActions.
Personal Access Token is the token to access the repo as similar way using user credentials.
Branch, after providing all the above values click on the load branches then list would be updated in drop down. And select the respective branch here.
WorkFlowID, select the .yaml to be loaded in the worlflow field.
By click on FetchWorkflow, the respective workflow will be updated in the field as well status of last 30 jobs also updated in commit and status field.

Update WorkFlow will push the existing code in to respective repo. That trigger the workflow to start the job provided in the workflow has on push command else only updates the workflow.
Trigger Workflow, this button will be active/works when workflow has the on_workflow_dispatch. It triggers to start the job.
Get Report post execution of testcases generated reports will be upload in artifacts of respective jobs. In case execution has been done on remote desktop it will be difficult to fetch the report hence this tool will make feasibility of fetching the report.

