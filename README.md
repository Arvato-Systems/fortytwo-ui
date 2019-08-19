# fortytwo-ui
ZK based front end of the Arvato Systems t9t framework

Program icons and application / branding logos can be set in downstream projects.

1) Add application_logo.png and company_logo.png into src/main/webapp/img/logo/ for the logo to be displayed in Login screen.
2) Set version in src/main/webapp/WEB-INF/resources/configuration.properties to display version of the customization
3) to set sub.title on login page/ forgot password and tenant selection page. add 'login.sub.title' key into your label.properties
4) to set the url when clicking the logo in home screen. add 'login.t9tapplication.url' key into your label.properties
5) Icons for the main menu should go into src/main/webapp/img/menu/

# Building
You need to compile zk-ckeditor locally, it is not on maven central. The sources can be obtained from https://github.com/zkoss/zkckeditor
Or get it from the ZKOSS maven repositories.
If you do not have ZK EE, then build with -DZKCE. (Some controls such as a multi selection dropdown will be missing in this case.)
