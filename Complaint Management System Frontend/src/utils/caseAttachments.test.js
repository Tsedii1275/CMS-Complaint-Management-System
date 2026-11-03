import { collectAttachmentGroups } from './attachments';

describe('collectAttachmentGroups', () => {
  it('returns nothing for an empty case', () => {
    expect(collectAttachmentGroups({})).toEqual([]);
    expect(collectAttachmentGroups(undefined)).toEqual([]);
  });

  it('gathers files from every stage of the case', () => {
    const groups = collectAttachmentGroups({
      complaint: { evidenceFiles: [{ url: '/f/evidence', name: 'evidence.pdf' }] },
      voiceFiles: [{ url: '/f/voice', name: 'call.mp3' }],
      workUnitFiles: [{ url: '/f/wu', name: 'resolution.docx' }],
      investigationFiles: [{ url: '/f/review', name: 'review.pdf' }],
      cxFiles: [{ url: '/f/cxo', name: 'cxo.pdf' }],
      auditAttachments: [{ url: '/f/audit', name: 'audit.pdf' }],
      committeeAttachment: '/f/committee',
      committeeAttachmentName: 'minutes.pdf'
    });

    expect(groups.map((g) => g.key)).toEqual(['complaint', 'voice', 'workUnit', 'investigation', 'audit', 'committee']);
    const investigation = groups.find((g) => g.key === 'investigation');
    expect(investigation.files.map((f) => f.name)).toEqual(['review.pdf', 'cxo.pdf']);
    expect(groups.find((g) => g.key === 'committee').files[0].name).toBe('minutes.pdf');
  });

  it('lists a file only once even when several fields hold it', () => {
    const groups = collectAttachmentGroups({
      evidenceFiles: [{ url: '/f/one', name: 'one.pdf' }],
      complaint: { evidenceFiles: [{ url: '/f/one', name: 'one.pdf' }] },
      investigationFiles: [{ url: '/f/one', name: 'one.pdf' }, { url: '/f/two', name: 'two.pdf' }],
      reviewFiles: [{ url: '/f/two', name: 'two.pdf' }]
    });

    const all = groups.flatMap((g) => g.files.map((f) => f.url));
    expect(all).toEqual(['/f/one', '/f/two']);
  });
});
