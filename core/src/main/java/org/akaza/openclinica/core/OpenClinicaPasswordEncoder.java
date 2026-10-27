/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * copyright (C) 2003 - 2011 Akaza Research
 * copyright (C) 2003 - 2019 OpenClinica
 * copyright (C) 2020 - 2024 LibreClinica
 */
package org.akaza.openclinica.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.password.PasswordEncoder;

public class OpenClinicaPasswordEncoder implements PasswordEncoder {

    PasswordEncoder currentPasswordEncoder;
    PasswordEncoder oldPasswordEncoder;
    /**
     * Accepted, never produced here.
     *
     * The AccuraTrial API writes user_account.passwd with bcrypt. It used to write an unsalted MD5 of the real
     * password so that an account it created could still sign in to this application, whose old encoder accepts
     * MD5; that put a trivially crackable copy of every password in the database. The API writes bcrypt now, so
     * this accepts bcrypt as well, and an account created or changed through the API can still sign in here.
     *
     * This is additive. Nothing that worked before stops working: a password this application wrote is still SHA-1
     * (currentPasswordEncoder), a password written before that is still MD5 (oldPasswordEncoder), and both are
     * still tried. encode() is deliberately unchanged, so this application keeps writing what it always wrote and
     * a SOAP user's stored hash stays the plain SHA-1 that WS-Security needs (see soapEncode and
     * SecurityManager.encryptPassword).
     */
    PasswordEncoder bcryptPasswordEncoder;

    public OpenClinicaPasswordEncoder() {
        // NOOP
    }

    public PasswordEncoder getCurrentPasswordEncoder() {
        return currentPasswordEncoder;
    }

    public void setCurrentPasswordEncoder(PasswordEncoder currentPasswordEncoder) {
        this.currentPasswordEncoder = currentPasswordEncoder;
    }

    public PasswordEncoder getOldPasswordEncoder() {
        return oldPasswordEncoder;
    }

    public void setOldPasswordEncoder(PasswordEncoder oldPasswordEncoder) {
        this.oldPasswordEncoder = oldPasswordEncoder;
    }

    public PasswordEncoder getBcryptPasswordEncoder() {
        return bcryptPasswordEncoder;
    }

    public void setBcryptPasswordEncoder(PasswordEncoder bcryptPasswordEncoder) {
        this.bcryptPasswordEncoder = bcryptPasswordEncoder;
    }

    public String soapEncode(CharSequence rawPassword) throws NoSuchAlgorithmException {
        String hexResult;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(Utf8.encode(rawPassword));
        hexResult = new String(Hex.encode(digest.digest()));

        return hexResult;
    }

	@Override
	public String encode(CharSequence rawPassword) {
		return currentPasswordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // bcrypt first, and only for a hash that is one: a bcrypt hash carries its own salt and cost in its
        // prefix, so asking a digest encoder about it would be meaningless, and asking bcrypt about a 40 or 32
        // character digest costs a key derivation for a certain no. An encoder left unset leaves this untouched.
        if (bcryptPasswordEncoder != null && isBcryptHash(encodedPassword)) {
            return bcryptPasswordEncoder.matches(rawPassword, encodedPassword);
        }
        boolean result = currentPasswordEncoder.matches(rawPassword, encodedPassword);
        result = result || oldPasswordEncoder.matches(rawPassword, encodedPassword);
		return result;
	}

    /**
     * A modular crypt bcrypt hash: $2a$, $2b$ or $2y$, a two digit cost, a $, then 53 characters of salt and
     * digest, 60 in all. Checked character by character rather than by regular expression so the shape of the
     * test is readable and cannot be changed by an escaping mistake.
     */
    static boolean isBcryptHash(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.length() != 60) {
            return false;
        }
        if (encodedPassword.charAt(0) != '$' || encodedPassword.charAt(1) != '2' || encodedPassword.charAt(3) != '$') {
            return false;
        }
        char variant = encodedPassword.charAt(2);
        if (variant != 'a' && variant != 'b' && variant != 'y') {
            return false;
        }
        return Character.isDigit(encodedPassword.charAt(4))
            && Character.isDigit(encodedPassword.charAt(5))
            && encodedPassword.charAt(6) == '$';
    }

}
