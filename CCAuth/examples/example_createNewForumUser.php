<?php
// get passed HTTP parameters
$encryptedUserName = $_GET["u"];     // user name
$encryptedUserPassword = $_GET["p"]; // password
$encryptedUserEmail = $_GET["e"]; // email

$secret_key = "01234567890ABCDE"; 	// SECRET key, same as in JAVA. 16 Bytes means AES-128, 32 Bytes means AES 256. Best in HEX.
$cipher     = "rijndael-128";

// include needed forum files here

// include AES_API.php
require_once ('AES_API.php');

//echo "encryptedUserName: [" . $encryptedUserName . "] encryptedUserPassword: [" . $encryptedUserPassword . "] encryptedUserEmail: [" . $encryptedUserEmail . "]";

// create new member ====================================

// decrypt received user data
$plainUserName = cryptare(hex2bin($encryptedUserName), $secret_key, $cipher, 0);
$plainUserPassword = cryptare(hex2bin($encryptedUserPassword), $secret_key, $cipher, 0);
$plainUserEmail = cryptare(hex2bin($encryptedUserEmail), $secret_key, $cipher, 0);

//echo "plainUserName: [" . $plainUserName . "] plainUserPassword: [" . $plainUserPassword . "] plainUserEmail: [" . $plainUserEmail . "]";

// check if account already exists in forum here

//echo "checkByName: [" . $checkMemberByName['member_id'] . "] checkByDisplayName: [" . $checkMemberByDisplayName['member_id'] . "] checkMemberByEmail: [" . $checkMemberByEmail['member_id'] . "]";

// only create account if no other user with same name, email or dsplay name exists
if (('' == $checkMemberByName['member_id']) AND ('' == $checkMemberByDisplayName['member_id']) AND ('' == $checkMemberByEmail['member_id']))
{
	// create account here
	
	//echo "User: " . $plainUserName . " PW: " . $plainUserPassword . " eMail: " . $plainUserEmail;
	// return user name for plugin
	echo $newUser['name'];
}
else
{
	echo '';
}
?>