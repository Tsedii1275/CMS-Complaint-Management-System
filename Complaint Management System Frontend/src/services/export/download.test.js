import { downloadBlob } from './download';

describe('downloadBlob', () => {
  let createObjectURL;
  let revokeObjectURL;
  let click;

  beforeEach(() => {
    jest.useFakeTimers();
    createObjectURL = jest.fn(() => 'blob:cms-export');
    revokeObjectURL = jest.fn();
    URL.createObjectURL = createObjectURL;
    URL.revokeObjectURL = revokeObjectURL;
    click = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  });

  afterEach(() => {
    click.mockRestore();
    jest.useRealTimers();
  });

  test('a csv is saved once and Firefox is not told to open it', () => {
    downloadBlob('Unique ID,Customer\nDBC-1,Abebe', 'Ethics_and_Conduct_2026-11-04.csv', 'text/csv;charset=utf-8;');

    jest.advanceTimersByTime(0);
    expect(click).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).not.toHaveBeenCalled();
    const blob = createObjectURL.mock.calls[0][0];
    expect(blob.type).toBe('application/csv');
    const link = document.querySelector('a');
    expect(link.download).toBe('Ethics_and_Conduct_2026-11-04.csv');
    expect(link.type).toBe('');

    jest.advanceTimersByTime(40000);
    expect(revokeObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:cms-export');
  });

  test('an xlsx keeps the Excel file type', () => {
    downloadBlob(
      new Uint8Array([80, 75, 3, 4]),
      'Ethics_and_Conduct_2026-11-04.xlsx',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );

    jest.advanceTimersByTime(0);
    const blob = createObjectURL.mock.calls[0][0];
    expect(blob.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    expect(document.querySelector('a').download).toBe('Ethics_and_Conduct_2026-11-04.xlsx');
    expect(click).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).not.toHaveBeenCalled();
  });
});