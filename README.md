# spotinst-plugin
Jenkins plugin to integrate with Spotinst


# Spotinst Elastigroup
This plugin uses [Spotinst](http://www.spotinst.com) Elastigroup to launch instances instead of directly launching them by itself.
The Elastigroup will make sure the slaves target capacity is being maintained.
More details can be found in our[Blog post](http://blog.spotinst.com/2016/06/08/elastigroup-jenkins).

#Usage

You'll need a Spotinst account to use this plugin, you can get one at [Spotinst Signup](https://spotinst.com/signup/).
Once you have an account, login into your [Spotinst Console] (https://console.spotinst.com/) to generate API token (Settings \-> Personal Access Tokens \->[Generate] (https://console.spotinst.com/#/settings/tokens)

Now, Create an Elastigroup with your proper Region, AMI, Instance Types etc.

After you installed the plugin, navigate to the main&nbsp; "Manage Jenkins" > "Configure System" page, and scroll down to the “Spotinst” Section, and add the API Token you’ve generated in the previous step. Then click "Validate Token", make sure that the token is valid.

Once you’ve set the Spotinst Token, scroll down near the bottom to the “Cloud” section.
There, Click on the "Add a new cloud" button, and select the "Spotinst"
Now -- you should Specify your "Elastigroup ID" and "Idle minutes before termination".
