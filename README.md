# spotinst-plugin
Jenkins plugin to integrate with Spotinst

Allow Jenkins to start slaves with
http://www.spotinst.com/[Spotinst] framework, and kill them as they get
unused.

# Spotinst Elasitgroup

This plugin uses http://www.spotinst.com/[Spotinst] Elastigroup to
launch instances instead of directly launching them by itself. +
The Elastigroup will make sure the slaves target capacity is being
maintained. +
More details can be found in
our http://blog.spotinst.com/2016/06/08/elastigroup-jenkins/[Blog post].

# Usage

You'll need a Spotinst account to use this plugin, you can get one
at https://spotinst.com/signup/[Spotinst Signup].

Once you have an account, login into
your https://console.spotinst.com/[Spotinst Console] to generate API
token (Settings -> Personal Access Tokens
-> https://console.spotinst.com/#/settings/tokens[Generate])

Now, Create an Elastigroup with your proper Region, AMI, Instance Types
etc.

After you installed the plugin, navigate to the main "Manage Jenkins" >
"Configure System" page, and scroll down to the “Spotinst” Section, and
add the *API Token* you’ve generated in the previous step. Then click
on "Validate Token", make sure that *the token is valid*.

Once you’ve set the Spotinst Token, scroll down near the bottom to the
“Cloud” section. +
There, Click on the "Add a new cloud" button, and select
the "Spotinst" option. +
Now – you should Specify your "Elastigroup ID" and "Idle minutes before
termination"

# Version history

=== Version 2.0.21 (May 31, 2020)

* Add AWS instance types


=== Version 2.0.19 (Feb 4, 2020)

* Add AWS instance types


=== Version 2.0.18 (Dec 22, 2019)

* Performance improvements for offline nodes.

=== Version 2.0.17 (Sep 12, 2019)

* Add missing AWS instance types

=== Version 2.0.16 (Aug 13, 2019)

* Support for Elastigroups from multiple Spotinst accounts
* Add Environment Variables and Tool Locations properties to the
Elastigroup cloud configuration

=== Version 2.0.15 (Jun 13, 2019)

* Add support for AWS instance types  - i3en, m5ad and r5ad

=== Version 2.0.14 (May 8, 2019)

* Add support for AWS instance type  - t3a

=== Version 2.0.13 (Mar 12, 2019)

* SpotinstToken config - add params to context for Jenkins configuration
as code plugin use

=== Version 2.0.12 (Feb 19, 2019)

* Add support for new AWS instance types

=== Version 2.0.11 (Feb 10, 2019)

* Add missing setters to support Jenkins configuration as code plugin

=== Version 2.0.10 (Nov 15, 2018)

* Fix AWS instance weight for older configurations

=== Version 2.0.9 (Nov 14, 2018)

* Add support for AWS instance types  - r5d

=== Version 2.0.8 (Nov 14, 2018)

* Add support for AWS instance types  - m5a, r5a +

=== Verson 2.0.7 (Nov 6, 2018)

* Support new version of azure Elastigroup

=== Version 2.0.6 (Oct 8, 2018)

* Performance improvements in scaling up instances
* Fix typo in logs

=== Version 2.0.5 (Jul 1, 2018)

* Add support for AWS instance types  - c5d, m5d

=== Version 2.0.4 (Jan 31, 2018)

* Add support for AWS instance types 

=== Version 2.0.3 (Jan 4, 2018)

* Performance Improvements

=== Version 2.0.2 (Oct 19, 2017)

* Performance Improvements

=== Version 2.0.1 (Jun 26, 2017)

* Add Spotinst Account Id configuration
* Support for Azure Elastigroups , slaves can run on Azure virtual
machines

=== Version 2.0.0 (Mar 23, 2017)

* *** New Major version - breaking changes (*you will need to
reconfigure Spotinst token and Spotinst cloud*) ***
* Support for idle slave termination according to billing hour
* Support for multiple labels in each slave
* Performance Improvements

=== Version 1.2.7 (Feb 28, 2017)

* Support for 'Tunnel connection through' option for slave connection to
master 
* Add JVM options for slaves

=== Version 1.2.6 (Feb 20, 2017)

* Support for nodes usage mode (NORMAL / EXCLUSIVE)
* Add AWS new instance types

=== Version 1.2.5 (Feb 1, 2017)

* Performance Improvements
* Support slave connection with credentials from thirdParty (GitHub)

=== Version 1.2.4 (Sep 5, 2016)

* Performance Improvements

=== Version 1.2.3 (Aug 30, 2016)

* Support for GCP (Google Cloud Platform) Elastigroups , slaves can run
on GCE instances 

=== Version 1.2.2 (Aug 21, 2016)

* Support for recovering spot slaves 

=== Version 1.2 (Jul 18, 2016)

* Add 'Remote root directory' and 'Instance type weight' to Cloud
config 
* Performance Improvements

=== Version 1.1 (Jul 5, 2016)

* Fixed the support for labels
* Performance Improvements

=== Version 1.0 (Jun 8, 2016)

* Initial release
