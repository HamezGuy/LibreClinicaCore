/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * copyright (C) 2003 - 2011 Akaza Research
 * copyright (C) 2003 - 2019 OpenClinica
 * copyright (C) 2020 - 2024 LibreClinica
 */
package org.akaza.openclinica.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;

/**
 * This encoder decides whether a stored password matches, and it now has three formats to consider rather than
 * two. What these cases protect is that adding the third took nothing away: an account whose stored hash was
 * written by this application (SHA-1) or by a version of it older still (MD5) must sign in exactly as it did
 * before, because the alternative is telling real people their password stopped working.
 *
 * The third format is bcrypt, which the AccuraTrial API writes into user_account.passwd. It used to write an
 * unsalted MD5 of the real password there, purely so an account it created could sign in here, which meant every
 * password this platform ever set also sat in the database in a form that is recovered in seconds.
 *
 * The hashes built here are built the way the database actually holds them: a bare hex digest, which is what
 * soapEncode writes and what the API wrote. MessageDigestPasswordEncoder.encode() salts with a random value, so
 * nothing below compares two of its outputs to each other.
 */
@SuppressWarnings("deprecation")
public class OpenClinicaPasswordEncoderTest {

    private static final String PASSWORD = "Synthetic-fixture-password-1!";

    private OpenClinicaPasswordEncoder encoder() {
        OpenClinicaPasswordEncoder encoder = new OpenClinicaPasswordEncoder();
        encoder.setCurrentPasswordEncoder(new MessageDigestPasswordEncoder("SHA-1"));
        encoder.setOldPasswordEncoder(new MessageDigestPasswordEncoder("MD5"));
        encoder.setBcryptPasswordEncoder(new BCryptPasswordEncoder());
        return encoder;
    }

    /** A bare hex digest, the shape user_account.passwd holds. */
    private static String hexDigest(String algorithm, String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(Utf8.encode(raw));
        return new String(Hex.encode(digest.digest()));
    }

    @Test
    public void acceptsTheBcryptHashTheApiNowWrites() {
        String stored = new BCryptPasswordEncoder().encode(PASSWORD);
        assertTrue(encoder().matches(PASSWORD, stored));
        assertFalse(encoder().matches("not the password", stored));
    }

    @Test
    public void stillAcceptsTheShaThisApplicationWrote() throws Exception {
        String stored = hexDigest("SHA-1", PASSWORD);
        assertTrue(encoder().matches(PASSWORD, stored));
        assertFalse(encoder().matches("not the password", stored));
    }

    @Test
    public void stillAcceptsALegacyMd5() throws Exception {
        String stored = hexDigest("MD5", PASSWORD);
        assertTrue(encoder().matches(PASSWORD, stored));
        assertFalse(encoder().matches("not the password", stored));
    }

    @Test
    public void stillAcceptsASaltedDigestWrittenThroughEncode() {
        // encode() salts, matches() reads the salt back out of the stored value. Both halves still work.
        assertTrue(encoder().matches(PASSWORD, encoder().encode(PASSWORD)));
    }

    @Test
    public void keepsWritingWhatItAlwaysWrote() throws Exception {
        // encode() is deliberately untouched: this application keeps writing a SHA-1 digest, not bcrypt.
        String written = encoder().encode(PASSWORD);
        assertTrue(new MessageDigestPasswordEncoder("SHA-1").matches(PASSWORD, written));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash(written));
        // A SOAP user's stored hash has to stay the bare SHA-1 that WS-Security computes from the raw password
        // (see SecurityManager.encryptPassword), and that hash has to keep matching.
        String soap = encoder().soapEncode(PASSWORD);
        assertTrue(soap.equals(hexDigest("SHA-1", PASSWORD)));
        assertTrue(encoder().matches(PASSWORD, soap));
    }

    @Test
    public void anUnsetBcryptEncoderChangesNothing() throws Exception {
        // A deployment whose context predates the new bean must behave exactly as it did.
        OpenClinicaPasswordEncoder encoder = new OpenClinicaPasswordEncoder();
        encoder.setCurrentPasswordEncoder(new MessageDigestPasswordEncoder("SHA-1"));
        encoder.setOldPasswordEncoder(new MessageDigestPasswordEncoder("MD5"));
        assertTrue(encoder.matches(PASSWORD, hexDigest("SHA-1", PASSWORD)));
        assertTrue(encoder.matches(PASSWORD, hexDigest("MD5", PASSWORD)));
    }

    @Test
    public void recognisesOnlyAWellFormedBcryptHash() throws Exception {
        assertTrue(OpenClinicaPasswordEncoder.isBcryptHash(new BCryptPasswordEncoder().encode(PASSWORD)));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash(null));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash(""));
        // A 32 character MD5 and a 40 character SHA-1 are the two it must never claim.
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash(hexDigest("MD5", PASSWORD)));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash(hexDigest("SHA-1", PASSWORD)));
        // Right length, wrong shape.
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash("$2c$10$012345678901234567890123456789012345678901234567890123"));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash("$2a$1x$012345678901234567890123456789012345678901234567890123"));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash("x2a$10$012345678901234567890123456789012345678901234567890123"));
        assertFalse(OpenClinicaPasswordEncoder.isBcryptHash("$2a$10012345678901234567890123456789012345678901234567890123$"));
    }
}
