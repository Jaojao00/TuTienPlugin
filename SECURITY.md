# 🔒 Security Policy

## Supported Versions

| Version | Supported         |
| ------- | ----------------- |
| 1.0.x   | ✅ Active support |

## Reporting a Vulnerability

If you discover a security vulnerability in **TuTienPlugin**, please report it responsibly.

### 📧 How to Report

1. **DO NOT** open a public GitHub issue for security vulnerabilities.
2. Send a detailed report via **email** to: `tainguyenhr.dev@gmail.com`
3. Or use [GitHub Private Vulnerability Reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) if available on this repository.

### 📝 What to Include

- Description of the vulnerability
- Steps to reproduce
- Affected version(s)
- Potential impact (e.g., data loss, privilege escalation, economy exploit)
- Suggested fix (if any)

### ⏱️ Response Timeline

| Stage              | Timeframe                     |
| ------------------ | ----------------------------- |
| Acknowledgment     | Within **48 hours**           |
| Initial assessment | Within **1 week**             |
| Patch release      | Within **2 weeks** (critical) |

## Known Security Considerations

### Server-Side

| Area                   | Risk                 | Mitigation                                                              |
| ---------------------- | -------------------- | ----------------------------------------------------------------------- |
| **Admin Commands**     | Privilege escalation | Protected by `tutien.admin` permission node                             |
| **Economy (Vault)**    | Duplication exploits | All transactions are atomic; GUI clicks are cancelled before processing |
| **Player Data (YML)**  | Data corruption      | Auto-save on disable; per-player file isolation                         |
| **WorldGuard Regions** | Bypass alchemy zone  | Region check on both GUI open and craft action                          |

### Configuration Safety

- All YML config files are **server-side only** — players cannot modify them.
- Item duplication is prevented by cancelling `InventoryClickEvent` before any item transfer.
- Economy operations use Vault's transactional API to prevent double-spending.

### Permissions

| Permission     | Description                                                    | Default  |
| -------------- | -------------------------------------------------------------- | -------- |
| `tutien.admin` | Access to admin commands (set stats, change ranks)             | OP only  |
| No permission  | All player commands (`/tutien menu`, `/tutien luyendan`, etc.) | Everyone |

## Best Practices for Server Admins

1. **Keep the plugin updated** to the latest version.
2. **Restrict `/tutien admin`** commands to trusted staff only.
3. **Back up player data** regularly (`plugins/TuTienPlugin/playerdata/`).
4. **Use WorldGuard** to define alchemy zones and prevent exploits in controlled areas.
5. **Monitor economy** — check for unusual balance changes that could indicate exploits.
6. **Review config files** after updates — new fields may have insecure defaults.

## Disclaimer

This plugin is provided **as-is** without warranty. The author is not responsible for data loss, server crashes, or economy imbalances resulting from misconfiguration or undiscovered bugs. Always test on a staging server before deploying to production.

---

> Thank you for helping keep TuTienPlugin secure! 🛡️
