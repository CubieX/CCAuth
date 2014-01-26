<?php
$encryptedUserName = $_GET["u"];
$encryptedUserPassword = $_GET["p"];
$plainUserName = "";
$plainUserPasword = "";

$secret_key = "01234567890ABCDE"; 	// SECRET key, same as in JAVA. 16 Bytes means AES-128, 32 Bytes means AES 256. Best in HEX.
$cipher     = "rijndael-128";

// include AES_API.php
require_once ('AES_API.php');

// BEGIN ACTIONS ================================================================

// Get User data from DB
//echo " Decrypting user name: " . $encryptedUserName . " ...";
$plainUserName = cryptare(hex2bin($encryptedUserName), $secret_key, $cipher, 0);
$plainUserPassword = cryptare(hex2bin($encryptedUserPassword), $secret_key, $cipher, 0);
//echo " Decrypted user name to : " . $plainUserName;
//echo " DB UserData: " . sqlGetPlayerData($plainUserName);
$playerData = sqlGetUserData($plainUserName);
// create hash of PW (PW is already HTML escaped by Java part!)
$pwHash = md5($plainUserPassword, FALSE);
$saltHash = md5($playerData['members_pass_salt'], FALSE);
$hash = md5($saltHash . $pwHash);

if($hash == $playerData['members_pass_hash'])
{
	echo $playerData['name']; // respond with player name, because PW was valid
}
else
{
	echo ''; // response with empty string, because PW hash was not found
}
?>