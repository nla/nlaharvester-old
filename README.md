NLA Harvester
=============

NLA Harvester is an OAI harvester with a web front end designed for easily managing harvests from multiple repositories.
Features:

*    Completely web based management
*    Supports user authentication with any backend supported by Spring Security.
*    Supports managing multiple harvester instances harvesting into different locations.
*    Built with a plugin architecture, so harvested records can be sent anywhere.
*    Supports viewing and visualizing harvested records within the web interface.
*    Has a pipeline architecture so that harvested records can be manipulated, converted or filtered.
*    Can handle complex harvest schedules such as only harvesting from a particular contributor on certain days, dates or times.
*    Supports email notifications for harvest statuses.
*    Contains a reporting mechanism.

See the wiki for instructions on getting a basic setup going on your own system.
Note that for production use, the NLA Harvester should be configured with plugins so that It knows what to do with the harvested data. Without any plugins, it just stores the harvested records internally, and makes them downloadable on demand.

