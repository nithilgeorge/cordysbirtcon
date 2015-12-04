# Change Log #

Cordys BIRT Reporting connector change log

# Version 2.0 #

  * Revamped the connector for BOP 4.2
  * Rewritten with BIRT Runtime version 4.2.

Improvements

---

Tenant aware
  * The new BOP 4.2 feature allows organization level web folders, the report files will be stored in the organization's web folder. Earlier the same folder that was used for all organization.
  * The BIRT service container need not be in the same organization as the web service. The service container can be in system organization and the web services can be in its own organization.

Changes

---


  * The engine home directory need not be set. It is removed from service container configuration page. All the other configuration remains the same.

Fixes

---

  * No more folder problem in linux. (web or Web)