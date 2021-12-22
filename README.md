# spotinst-plugin
Jenkins plugin to integrate with Spot.


# Spot Elastigroup
This plugin uses [Spot](http://www.spotinst.com) Elastigroup to launch instances instead of launching them directly.
The Elastigroup will make sure the agents' target-capacity is being maintained.
More details can be found in our [documentation](https://docs.spot.io/tools-and-provisioning/ci-cd/jenkins?id=jenkins).

#Usage

You'll need a Spot account to use this plugin, you can get one at [Spot Sign-up](https://spotinst.com/signup/).
Once you have an account, login to [Spot Console] (https://console.spotinst.com/) to generate an API token:


Settings -> Personal Access Tokens -> [Generate] (https://console.spotinst.com/#/settings/tokens)


Now, create an Elastigroup with your proper Region, AMI, Instance Types etc.

After you installed the plugin, navigate to "Manage Jenkins" > "Configure System" page, and scroll down to the “Spot” Section, 
and add the API token you’ve generated in the previous step. Then, click "Validate Token" and make sure that the token is valid.

Once you’ve set the Spot Token, scroll down near the bottom to the “Cloud” section.
There, Click on the "Add a new cloud" button, and select the "Spot"
Now -- you should Specify your "Elastigroup ID" and "Idle minutes before termination", and other options you desire.
